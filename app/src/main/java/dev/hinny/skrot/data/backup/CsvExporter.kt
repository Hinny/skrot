package dev.hinny.skrot.data.backup

import dev.hinny.skrot.data.db.SkrotDatabase
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Workout-log CSV for spreadsheet users: one row per completed set. */
object CsvExporter {

    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else field

    suspend fun buildCsv(db: SkrotDatabase): String {
        val sets = db.sessionDao().allCompletedSets()
        val exercises = db.exerciseDao().getAll().associateBy { it.id }
        val gyms = db.gymDao().getAll().associateBy { it.id }
        val zone = ZoneId.systemDefault()

        val sb = StringBuilder()
        sb.append("date,exercise,muscle_group,equipment,measurement_type,set_type,load_kg,reps,rest_sec,gym,note\r\n")
        for (s in sets) {
            val e = exercises[s.exerciseId] ?: continue
            val date = dateFormat.format(Instant.ofEpochMilli(s.sessionDate).atZone(zone))
            val gym = s.sessionGymId?.let { gyms[it]?.name } ?: ""
            sb.append(
                listOf(
                    date,
                    e.nameEn,
                    e.muscleGroup.name,
                    e.equipment.joinToString("+") { it.name },
                    e.measurementType.name,
                    s.set.setType.name,
                    s.set.load.toString(),
                    s.set.reps.toString(),
                    s.set.restSec.toString(),
                    gym,
                    s.set.note,
                ).joinToString(",") { escape(it) }
            )
            sb.append("\r\n")
        }
        return sb.toString()
    }
}
