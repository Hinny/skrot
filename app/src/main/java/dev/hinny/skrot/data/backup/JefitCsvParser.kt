package dev.hinny.skrot.data.backup

import dev.hinny.skrot.data.model.WeightUnit
import dev.hinny.skrot.domain.Units
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Defensive parser for JEFIT's CSV export. The format is undocumented and varies
 * between versions, so columns are detected by header name, and both packed
 * ("60x8,60x8,60x7" per exercise per day) and one-row-per-set layouts are handled.
 */
object JefitCsvParser {

    data class ParsedSet(val loadKg: Double, val reps: Int)
    data class ParsedExercise(val name: String, val sets: MutableList<ParsedSet> = mutableListOf())
    data class ParsedSession(val date: LocalDate, val exercises: MutableList<ParsedExercise> = mutableListOf())

    data class Result(
        val sessions: List<ParsedSession>,
        val skipped: List<String>,
        /** Unit detected from headers, or null if undetectable (caller should ask the user). */
        val detectedUnit: WeightUnit?,
        val totalSets: Int,
    )

    private val packedEntry = Regex("""^(-?\d+(?:[.,]\d+)?)\s*[x×X*]\s*(\d+)$""")
    private val dateFormats = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,          // 2024-03-01
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("M/d/yyyy"),
        DateTimeFormatter.ofPattern("d.M.yyyy"),
    )

    /**
     * @param unitOverride force kg or lbs interpretation; null = use header detection,
     *   falling back to kg.
     */
    fun parse(csv: String, unitOverride: WeightUnit? = null): Result {
        val rows = readCsv(csv).filter { row -> row.any { it.isNotBlank() } }
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
                val packed = row[logsCol]
                for (entry in packed.split(',', ';')) {
                    val text = entry.trim()
                    if (text.isEmpty()) continue
                    val match = packedEntry.matchEntire(text)
                    if (match == null) {
                        skipped.add("Line $lineNo: unparseable set \"$text\"")
                        continue
                    }
                    val load = match.groupValues[1].replace(',', '.').toDouble()
                    val reps = match.groupValues[2].toInt()
                    sets.add(ParsedSet(toKg(load, unit), reps))
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
