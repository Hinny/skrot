package dev.hinny.skrot.data.backup

import dev.hinny.skrot.data.model.WeightUnit
import dev.hinny.skrot.domain.Units
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Defensive parser for JEFIT's CSV export. The format is undocumented and varies
 * between versions, so two shapes are handled:
 *
 * 1. The real "My Data" export: a multi-section text file where sections are
 *    delimited by banner lines like `### EXERCISE LOGS ####...`. Workout history
 *    is read from the EXERCISE LOGS section, body measurements from PROFILE, and
 *    the weight unit from SETTING.
 * 2. A plain single-table CSV (one header row), with either packed sets
 *    ("60x8,60x8,60x7" per exercise per day) or one row per set.
 */
object JefitCsvParser {

    data class ParsedSet(val loadKg: Double, val reps: Int)
    data class ParsedExercise(val name: String, val sets: MutableList<ParsedSet> = mutableListOf())
    data class ParsedSession(val date: LocalDate, val exercises: MutableList<ParsedExercise> = mutableListOf())

    /** One imported body-measurement entry (already converted to kg/cm). */
    data class ParsedBodyMetric(
        val date: LocalDate,
        val weightKg: Double? = null,
        val chestCm: Double? = null,
        val armsCm: Double? = null,
        val waistCm: Double? = null,
        val hipsCm: Double? = null,
        val thighsCm: Double? = null,
    ) {
        val fieldCount: Int
            get() = listOfNotNull(weightKg, chestCm, armsCm, waistCm, hipsCm, thighsCm).size
    }

    /** One exercise planned inside a routine day (target sets, no logged history). */
    data class ParsedPlannedExercise(
        val name: String,
        /** Exercises sharing this key (JEFIT's `superset` id, or their own id when standalone) form one block. */
        val supersetKey: Long,
        val sortOrder: Int,
        val restSec: Int,
        val sets: List<ParsedSet>,
    )

    data class ParsedDay(val name: String, val sortOrder: Int, val exercises: List<ParsedPlannedExercise>)
    data class ParsedRoutine(val name: String, val days: List<ParsedDay>)

    data class Result(
        val sessions: List<ParsedSession>,
        val skipped: List<String>,
        /** Unit detected from the file, or null if undetectable (caller should ask the user). */
        val detectedUnit: WeightUnit?,
        val totalSets: Int,
        val bodyMetrics: List<ParsedBodyMetric> = emptyList(),
        val routines: List<ParsedRoutine> = emptyList(),
    )

    private val packedEntry = Regex("""^(-?\d+(?:[.,]\d+)?)\s*[x×X*]\s*(\d+)$""")
    private val sectionBanner = Regex("""^#{2,}\s*([A-ZÅÄÖ][A-ZÅÄÖ0-9 &/_-]*?)\s*#*\s*$""")
    private val dateFormats = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,          // 2024-03-01
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("M/d/yyyy"),
        DateTimeFormatter.ofPattern("d.M.yyyy"),
    )

    /**
     * @param unitOverride force kg or lbs interpretation; null = use detection,
     *   falling back to kg.
     */
    fun parse(csv: String, unitOverride: WeightUnit? = null): Result {
        val sections = splitSections(csv)
        return if (sections != null) parseSectioned(sections, unitOverride)
        else parseSingleTable(csv, unitOverride)
    }

    // ---------------------------------------------------------------------
    // Multi-section export ("### EXERCISE LOGS ###" banners)
    // ---------------------------------------------------------------------

    /** Returns section name -> raw CSV text, or null when the file has no banners. */
    private fun splitSections(text: String): Map<String, String>? {
        val lines = text.lines()
        if (lines.none { sectionBanner.matches(it.trim()) }) return null

        val sections = linkedMapOf<String, StringBuilder>()
        var current: StringBuilder? = null
        for (line in lines) {
            val trimmed = line.trim()
            val match = sectionBanner.matchEntire(trimmed)
            when {
                match != null -> {
                    current = StringBuilder().also { sections[match.groupValues[1].uppercase()] = it }
                }
                trimmed.isNotEmpty() && trimmed.all { it == '#' } -> current = null // closing banner
                else -> current?.append(line)?.append('\n')
            }
        }
        return sections.mapValues { it.value.toString() }
    }

    private fun parseSectioned(sections: Map<String, String>, unitOverride: WeightUnit?): Result {
        val skipped = mutableListOf<String>()
        val detectedUnit = detectUnitFromSettings(sections["SETTING"])
        val unit = unitOverride ?: detectedUnit ?: WeightUnit.KG

        val (sessions, totalSets) = parseExerciseLogs(sections["EXERCISE LOGS"], unit, skipped)
        val bodyMetrics = parseProfile(sections["PROFILE"], unit, skipped)
        val routines = parseRoutines(sections["ROUTINES"], unit, skipped)

        if (sessions.isEmpty() && bodyMetrics.isEmpty() && routines.isEmpty()) {
            skipped.add(0, "No EXERCISE LOGS, PROFILE or ROUTINES data found in the JEFIT export")
        }
        return Result(sessions, skipped, detectedUnit, totalSets, bodyMetrics, routines)
    }

    /** The SETTING section has a `mass` column whose value is " kg" or " lbs". */
    private fun detectUnitFromSettings(text: String?): WeightUnit? {
        val rows = nonBlankRows(text ?: return null)
        if (rows.size < 2) return null
        val header = rows.first().map { it.trim().lowercase() }
        val massCol = header.indexOfFirst { it == "mass" || "mass" in it }
        if (massCol < 0) return null
        val value = rows[1].getOrNull(massCol)?.trim()?.lowercase() ?: return null
        return when {
            "kg" in value -> WeightUnit.KG
            "lb" in value -> WeightUnit.LBS
            else -> null
        }
    }

    private fun parseExerciseLogs(
        text: String?,
        unit: WeightUnit,
        skipped: MutableList<String>,
    ): Pair<List<ParsedSession>, Int> {
        val rows = nonBlankRows(text ?: return emptyList<ParsedSession>() to 0)
        if (rows.size < 2) return emptyList<ParsedSession>() to 0

        val header = rows.first().map { it.trim().lowercase() }
        val dateCol = header.indexOf("mydate").takeIf { it >= 0 }
            ?: header.indexOfFirst { "date" in it }
        val nameCol = header.indexOf("ename").takeIf { it >= 0 }
            ?: header.indexOfFirst { "exercise" in it }
        val logsCol = header.indexOf("logs")
        if (dateCol < 0 || nameCol < 0 || logsCol < 0) {
            skipped.add("EXERCISE LOGS section: missing mydate/ename/logs columns")
            return emptyList<ParsedSession>() to 0
        }

        val sessions = linkedMapOf<LocalDate, ParsedSession>()
        var totalSets = 0
        var timedRows = 0
        var emptyRows = 0
        var namelessRows = 0

        for (row in rows.drop(1)) {
            if (row == rows.first()) continue // repeated header, just in case
            val dateText = row.getOrNull(dateCol)?.trim().orEmpty()
            val name = row.getOrNull(nameCol)?.trim().orEmpty()
            val date = parseDate(dateText)
            if (date == null) {
                skipped.add("Exercise log: unparseable date \"$dateText\"")
                continue
            }
            if (name.isBlank()) { namelessRows++; continue }

            when (val packed = parsePacked(row.getOrNull(logsCol).orEmpty())) {
                is PackedRow.Timed -> timedRows++
                is PackedRow.Sets -> {
                    packed.unparseable.forEach { skipped.add("Exercise log $dateText \"$name\": unparseable set \"$it\"") }
                    val sets = packed.entries.map { ParsedSet(toKg(it.first, unit), it.second) }
                    if (sets.isEmpty()) { emptyRows++; continue }
                    val session = sessions.getOrPut(date) { ParsedSession(date) }
                    val exercise = session.exercises.lastOrNull { it.name.equals(name, ignoreCase = true) }
                        ?: ParsedExercise(name).also { session.exercises.add(it) }
                    exercise.sets.addAll(sets)
                    totalSets += sets.size
                }
            }
        }
        if (timedRows > 0) skipped.add("$timedRows timed/cardio/stretch entries skipped (no weight×reps data)")
        if (emptyRows > 0) skipped.add("$emptyRows entries without logged sets skipped")
        if (namelessRows > 0) skipped.add("$namelessRows entries without an exercise name skipped")
        return sessions.values.toList() to totalSets
    }

    /** PROFILE section: body measurements. Zero values mean "not measured" in JEFIT. */
    private fun parseProfile(
        text: String?,
        unit: WeightUnit,
        skipped: MutableList<String>,
    ): List<ParsedBodyMetric> {
        val rows = nonBlankRows(text ?: return emptyList())
        if (rows.size < 2) return emptyList()

        val header = rows.first().map { it.trim().lowercase() }
        val dateCol = header.indexOf("mydate").takeIf { it >= 0 } ?: header.indexOfFirst { "date" in it }
        if (dateCol < 0) return emptyList()
        fun col(name: String) = header.indexOf(name)
        val weightCol = col("weight")
        val chestCol = col("chest")
        val armsCol = col("arms")
        val waistCol = col("waist")
        val hipsCol = col("hips")
        val thighsCol = col("thighs")

        fun value(row: List<String>, index: Int): Double? {
            if (index < 0) return null
            val v = row.getOrNull(index)?.trim()?.replace(',', '.')?.toDoubleOrNull() ?: return null
            return if (v > 0.0) v else null
        }

        // Keep the richest entry per date (exports contain duplicated legacy rows).
        val byDate = linkedMapOf<LocalDate, ParsedBodyMetric>()
        for (row in rows.drop(1)) {
            val dateText = row.getOrNull(dateCol)?.trim().orEmpty()
            val date = parseDate(dateText)
            if (date == null) {
                skipped.add("Body log: unparseable date \"$dateText\"")
                continue
            }
            val metric = ParsedBodyMetric(
                date = date,
                weightKg = value(row, weightCol)?.let { toKg(it, unit) },
                chestCm = value(row, chestCol),
                armsCm = value(row, armsCol),
                waistCm = value(row, waistCol),
                hipsCm = value(row, hipsCol),
                thighsCm = value(row, thighsCol),
            )
            if (metric.fieldCount == 0) continue
            val existing = byDate[date]
            if (existing == null || metric.fieldCount > existing.fieldCount) byDate[date] = metric
        }
        return byDate.values.sortedBy { it.date }
    }

    /**
     * ROUTINES section: a flat sequence of single-row "tables" (their own
     * header row plus one data row, blank-row separated) nesting routine ->
     * day -> planned exercise purely by order of appearance. The three shapes
     * are told apart by a column unique to each: a routine row has `dayaweek`,
     * a day row has `dayIndex`, an exercise-in-plan row has `belongplan`.
     * Exercises sharing a non-zero `superset` value (JEFIT points every member
     * at one member's own id) form a single block/superset.
     *
     * Chunking is done on CSV rows (via [readCsv] over the whole section, once)
     * rather than on raw text lines, because JEFIT's routine `description`
     * field is a quoted value that can itself contain blank lines — splitting
     * the raw text on blank lines would cut such a record in half.
     */
    private fun parseRoutines(
        text: String?,
        unit: WeightUnit,
        skipped: MutableList<String>,
    ): List<ParsedRoutine> {
        if (text == null) return emptyList()

        val chunks = mutableListOf<List<List<String>>>()
        var current = mutableListOf<List<String>>()
        for (row in readCsv(text)) {
            if (row.all { it.isBlank() }) {
                if (current.isNotEmpty()) { chunks.add(current); current = mutableListOf() }
            } else {
                current.add(row)
            }
        }
        if (current.isNotEmpty()) chunks.add(current)

        val routines = mutableListOf<ParsedRoutine>()
        var routineName: String? = null
        var days = mutableListOf<ParsedDay>()
        var dayName: String? = null
        var daySort = 0
        var exercises = mutableListOf<ParsedPlannedExercise>()

        fun flushDay() {
            val name = dayName ?: return
            days.add(ParsedDay(name, daySort, exercises.toList()))
            exercises = mutableListOf()
            dayName = null
        }
        fun flushRoutine() {
            flushDay()
            val name = routineName ?: return
            if (days.isNotEmpty()) routines.add(ParsedRoutine(name, days.toList()))
            days = mutableListOf()
            routineName = null
        }

        for (rows in chunks) {
            if (rows.size < 2) continue
            val header = rows[0].map { it.trim() }
            val dataRows = rows.drop(1)
            when {
                "dayaweek" in header -> {
                    flushRoutine()
                    val nameIdx = header.indexOf("name")
                    val rawName = dataRows[0].getOrNull(nameIdx)?.trim().orEmpty()
                    routineName = rawName.ifBlank { "Imported routine" }
                }

                "belongplan" in header -> {
                    val idIdx = header.indexOf("_id")
                    val supersetIdx = header.indexOf("superset")
                    val nameIdx = header.indexOf("exercisename")
                    val timerIdx = header.indexOf("timer")
                    val logsIdx = header.indexOf("logs")
                    val sortIdx = header.indexOf("mysort")
                    for (row in dataRows) {
                        val exName = row.getOrNull(nameIdx)?.trim().orEmpty()
                        if (exName.isBlank()) continue
                        val ownId = row.getOrNull(idIdx)?.trim()?.toLongOrNull() ?: 0L
                        val superset = row.getOrNull(supersetIdx)?.trim()?.toLongOrNull() ?: 0L
                        val restSec = row.getOrNull(timerIdx)?.trim()?.toDoubleOrNull()?.toInt() ?: 0
                        val sortOrder = row.getOrNull(sortIdx)?.trim()?.toIntOrNull() ?: 0
                        val sets = when (val packed = parsePacked(row.getOrNull(logsIdx).orEmpty())) {
                            is PackedRow.Timed -> emptyList()
                            is PackedRow.Sets -> packed.entries.map { ParsedSet(toKg(it.first, unit), it.second) }
                        }
                        if (sets.isEmpty()) {
                            skipped.add("Routine exercise \"$exName\": no set data, skipped")
                            continue
                        }
                        exercises.add(
                            ParsedPlannedExercise(
                                name = exName,
                                supersetKey = if (superset != 0L) superset else ownId,
                                sortOrder = sortOrder,
                                restSec = restSec,
                                sets = sets,
                            )
                        )
                    }
                }

                "dayIndex" in header -> {
                    flushDay()
                    val nameIdx = header.indexOf("name")
                    val sortIdx = header.indexOf("sort_order")
                    val rawName = dataRows[0].getOrNull(nameIdx)?.trim().orEmpty()
                    dayName = rawName.ifBlank { "Day" }
                    daySort = dataRows[0].getOrNull(sortIdx)?.trim()?.toIntOrNull() ?: 0
                }
            }
        }
        flushRoutine()
        return routines
    }

    // ---------------------------------------------------------------------
    // Packed "60x8,60x8" strings
    // ---------------------------------------------------------------------

    private sealed class PackedRow {
        /** Cardio/stretch/timed encoding (e.g. "0x0,0x0,0x0,0x10,0x60") — no strength sets. */
        object Timed : PackedRow()
        data class Sets(
            /** (load, reps) pairs with reps > 0. */
            val entries: List<Pair<Double, Int>>,
            val unparseable: List<String>,
        ) : PackedRow()
    }

    private fun parsePacked(packed: String): PackedRow {
        val raw = packed.split(',', ';').map { it.trim() }.filter { it.isNotEmpty() }
        val matched = mutableListOf<Pair<Double, Int>>()
        val unparseable = mutableListOf<String>()
        var zeroRepEntries = 0
        for (entry in raw) {
            val match = packedEntry.matchEntire(entry)
            if (match == null) {
                unparseable.add(entry)
                continue
            }
            val load = match.groupValues[1].replace(',', '.').toDouble()
            val reps = match.groupValues[2].toInt()
            if (reps == 0) { zeroRepEntries++; continue }
            matched.add(load to reps)
        }
        // JEFIT encodes timed/cardio/stretch logs as 4+ zero-load fields such as
        // "0x0,0x0,0x0,0x10,0x1" or "0x0.0,...,0x60". Distinguish those from genuine
        // body-weight sets like "0x5,0x4,0x3" (few fields, all with real reps).
        val allZeroLoad = matched.all { it.first == 0.0 }
        val looksTimed = raw.size >= 4 && allZeroLoad && (zeroRepEntries > 0 || unparseable.isNotEmpty())
        if (looksTimed) return PackedRow.Timed
        return PackedRow.Sets(matched, unparseable)
    }

    // ---------------------------------------------------------------------
    // Legacy single-table CSV
    // ---------------------------------------------------------------------

    private fun parseSingleTable(csv: String, unitOverride: WeightUnit?): Result {
        val rows = nonBlankRows(csv)
        if (rows.isEmpty()) return Result(emptyList(), listOf("Empty file"), null, 0)

        val header = rows.first().map { it.trim().lowercase() }
        val dateCol = header.indexOfFirst { "date" in it }
        val nameCol = header.indexOfFirst { "ename" in it || "exercise" in it || it == "name" }
        val logsCol = header.indexOfFirst { "logs" in it || ("weight" in it && "rep" in it) }
        val weightCol = header.indexOfFirst { "weight" in it && "rep" !in it }
        val repsCol = header.indexOfFirst { "rep" in it && "weight" !in it }

        val detectedUnit = when {
            header.any { "lb" in it } -> WeightUnit.LBS
            header.any { "kg" in it } -> WeightUnit.KG
            else -> null
        }
        val unit = unitOverride ?: detectedUnit ?: WeightUnit.KG

        if (dateCol < 0 || nameCol < 0 || (logsCol < 0 && (weightCol < 0 || repsCol < 0))) {
            return Result(
                emptyList(),
                listOf("Unrecognized JEFIT CSV: missing date/exercise/set columns"),
                detectedUnit,
                0,
            )
        }

        val skipped = mutableListOf<String>()
        val sessions = linkedMapOf<LocalDate, ParsedSession>()
        var totalSets = 0

        for ((rowIndex, row) in rows.drop(1).withIndex()) {
            val lineNo = rowIndex + 2
            val dateText = row.getOrNull(dateCol)?.trim().orEmpty()
            val name = row.getOrNull(nameCol)?.trim().orEmpty()
            val date = parseDate(dateText)
            if (date == null) {
                skipped.add("Line $lineNo: unparseable date \"$dateText\"")
                continue
            }
            if (name.isBlank()) {
                skipped.add("Line $lineNo: missing exercise name")
                continue
            }

            val sets = mutableListOf<ParsedSet>()
            if (logsCol >= 0 && row.getOrNull(logsCol)?.isNotBlank() == true) {
                when (val packed = parsePacked(row[logsCol])) {
                    is PackedRow.Timed -> {
                        skipped.add("Line $lineNo: timed/cardio entry for \"$name\"")
                        continue
                    }
                    is PackedRow.Sets -> {
                        packed.unparseable.forEach { skipped.add("Line $lineNo: unparseable set \"$it\"") }
                        packed.entries.forEach { sets.add(ParsedSet(toKg(it.first, unit), it.second)) }
                    }
                }
            } else if (weightCol >= 0 && repsCol >= 0) {
                val load = row.getOrNull(weightCol)?.trim()?.replace(',', '.')?.toDoubleOrNull()
                val reps = row.getOrNull(repsCol)?.trim()?.toDoubleOrNull()?.toInt()
                if (load == null || reps == null) {
                    skipped.add("Line $lineNo: unparseable weight/reps")
                    continue
                }
                sets.add(ParsedSet(toKg(load, unit), reps))
            }

            if (sets.isEmpty()) {
                skipped.add("Line $lineNo: no sets for \"$name\"")
                continue
            }

            val session = sessions.getOrPut(date) { ParsedSession(date) }
            val exercise = session.exercises.lastOrNull { it.name.equals(name, ignoreCase = true) }
                ?: ParsedExercise(name).also { session.exercises.add(it) }
            exercise.sets.addAll(sets)
            totalSets += sets.size
        }

        return Result(sessions.values.toList(), skipped, detectedUnit, totalSets)
    }

    // ---------------------------------------------------------------------
    // Shared helpers
    // ---------------------------------------------------------------------

    private fun nonBlankRows(text: String): List<List<String>> =
        readCsv(text).filter { row -> row.any { it.isNotBlank() } }

    private fun toKg(load: Double, unit: WeightUnit): Double =
        if (unit == WeightUnit.LBS) Units.lbsToKg(load) else load

    private fun parseDate(text: String): LocalDate? {
        if (text.isBlank()) return null
        for (format in dateFormats) {
            try {
                return LocalDate.parse(text, format)
            } catch (_: Exception) {
                // try the next format
            }
        }
        return null
    }

    /** Minimal quote-aware CSV reader (handles quoted fields, escaped quotes, CRLF). */
    fun readCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var field = StringBuilder()
        var row = mutableListOf<String>()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes -> when {
                    c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                        field.append('"'); i++
                    }

                    c == '"' -> inQuotes = false
                    else -> field.append(c)
                }

                c == '"' -> inQuotes = true
                c == ',' -> {
                    row.add(field.toString()); field = StringBuilder()
                }

                c == '\r' -> { /* swallow; \n ends the row */ }

                c == '\n' -> {
                    row.add(field.toString()); field = StringBuilder()
                    rows.add(row); row = mutableListOf()
                }

                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows
    }
}
