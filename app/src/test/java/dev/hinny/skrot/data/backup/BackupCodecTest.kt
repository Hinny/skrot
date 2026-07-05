package dev.hinny.skrot.data.backup

import dev.hinny.skrot.data.model.BodyMetric
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.ExerciseGroup
import dev.hinny.skrot.data.model.Gym
import dev.hinny.skrot.data.model.GymExercise
import dev.hinny.skrot.data.model.LoggedSet
import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.MuscleGroup
import dev.hinny.skrot.data.model.PlannedExercise
import dev.hinny.skrot.data.model.PlannedSet
import dev.hinny.skrot.data.model.PrefillMode
import dev.hinny.skrot.data.model.Routine
import dev.hinny.skrot.data.model.RoutineDay
import dev.hinny.skrot.data.model.SessionExercise
import dev.hinny.skrot.data.model.SetType
import dev.hinny.skrot.data.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCodecTest {

    private fun sampleBackup() = BackupFile(
        schemaVersion = BackupCodec.SCHEMA_VERSION,
        exportedAt = 1_720_000_000_000,
        appVersion = "1.0.0",
        groups = listOf(ExerciseGroup(1, "Horizontal press", "Horisontell press")),
        exercises = listOf(
            Exercise(
                id = 1, nameEn = "Bench Press", nameSv = "Bänkpress",
                muscleGroup = MuscleGroup.CHEST,
                secondaryMuscles = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
                measurementType = MeasurementType.WEIGHT_KG, groupId = 1,
                nextTimeNote = "lower the seat one notch",
            ),
            Exercise(
                id = 2, nameEn = "Pull-Up", nameSv = "Räckhäv",
                muscleGroup = MuscleGroup.BACK,
                measurementType = MeasurementType.BODYWEIGHT, bodyweightFactor = 100,
            ),
        ),
        routines = listOf(
            Routine(
                id = 1, name = "PPL", description = "push pull legs",
                prefillMode = PrefillMode.HYBRID, isActive = true,
                tags = listOf("rebuild"), nextDayIndex = 1,
            )
        ),
        days = listOf(RoutineDay(id = 1, routineId = 1, name = "Push", weekdays = listOf(1, 4))),
        plannedExercises = listOf(PlannedExercise(id = 1, dayId = 1, exerciseId = 1)),
        plannedSets = listOf(
            PlannedSet(
                id = 1, plannedExerciseId = 1, setType = SetType.STANDARD,
                targetRepsMin = 8, targetRepsMax = 12, targetLoad = 60.0, restSec = 120,
            )
        ),
        gyms = listOf(Gym(id = 1, name = "Campushallen", isDefault = true)),
        gymExercises = listOf(GymExercise(1, 1)),
        sessions = listOf(
            WorkoutSession(
                id = 1, startedAt = 1_719_000_000_000, endedAt = 1_719_003_600_000,
                routineId = 1, routineDayId = 1, gymId = 1, lastActivityAt = 1_719_003_500_000,
                note = "good session",
            )
        ),
        sessionExercises = listOf(
            SessionExercise(id = 1, sessionId = 1, exerciseId = 1, plannedExerciseId = 1)
        ),
        loggedSets = listOf(
            LoggedSet(
                id = 1, sessionExerciseId = 1, load = 60.0, reps = 10,
                completed = true, restSec = 120, completedAt = 1_719_001_000_000,
            )
        ),
        bodyMetrics = listOf(BodyMetric(id = 1, date = 1_719_000_000_000, weightKg = 82.5)),
    )

    @Test
    fun `encode then decode returns an identical backup`() {
        val original = sampleBackup()
        val decoded = BackupCodec.decode(BackupCodec.encode(original))
        assertEquals(original, decoded)
    }

    @Test
    fun `unknown json keys are ignored for forward compatibility`() {
        val text = BackupCodec.encode(sampleBackup())
            .replaceFirst("{", "{\"someFutureField\":42,")
        assertEquals(sampleBackup(), BackupCodec.decode(text))
    }

    @Test
    fun `garbage input is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode("not json at all")
        }
    }

    @Test
    fun `newer schema versions are rejected`() {
        val newer = sampleBackup().copy(schemaVersion = BackupCodec.SCHEMA_VERSION + 1)
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode(BackupCodec.encode(newer))
        }
    }

    @Test
    fun `csv field escaping`() {
        assertEquals("plain", CsvExporter.escape("plain"))
        assertEquals("\"a,b\"", CsvExporter.escape("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvExporter.escape("say \"hi\""))
    }
}
