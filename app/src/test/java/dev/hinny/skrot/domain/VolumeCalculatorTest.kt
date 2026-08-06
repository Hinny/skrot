package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.LoggedSet
import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.MuscleGroup
import dev.hinny.skrot.data.model.SessionExercise
import dev.hinny.skrot.data.model.SessionExerciseWithDetails
import dev.hinny.skrot.data.model.SessionWithContent
import dev.hinny.skrot.data.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VolumeCalculatorTest {

    @Test
    fun `weight volume is load times reps`() {
        assertEquals(
            500.0,
            VolumeCalculator.setVolumeKg(MeasurementType.WEIGHT_KG, 100.0, 5, 75.0, 100)!!,
            0.0,
        )
    }

    @Test
    fun `bodyweight volume uses bodyweight times factor plus added weight`() {
        // (80 * 0.65 + 10) * 10 = 620
        assertEquals(
            620.0,
            VolumeCalculator.setVolumeKg(MeasurementType.BODYWEIGHT, 10.0, 10, 80.0, 65)!!,
            0.001,
        )
    }

    @Test
    fun `assistance counts as negative added weight`() {
        // (80 * 1.0 - 20) * 5 = 300
        assertEquals(
            300.0,
            VolumeCalculator.setVolumeKg(MeasurementType.BODYWEIGHT, -20.0, 5, 80.0, 100)!!,
            0.001,
        )
    }

    @Test
    fun `machine levels are excluded from kg volume`() {
        assertNull(VolumeCalculator.setVolumeKg(MeasurementType.MACHINE_LEVEL, 7.0, 10, 75.0, 100))
    }

    @Test
    fun `default fallback bodyweight is 75 kg`() {
        assertEquals(75.0, VolumeCalculator.DEFAULT_BODYWEIGHT_FALLBACK_KG, 0.0)
    }

    // The session-wide totals the home recap, the finish dialog and the summary
    // screen all show. They used to walk the session themselves, three times.

    private fun exercise(id: Long, measurement: MeasurementType, factor: Int = 100) = Exercise(
        id = id,
        nameEn = "e$id",
        nameSv = "e$id",
        muscleGroup = MuscleGroup.CHEST,
        measurementType = measurement,
        bodyweightFactor = factor,
    )

    private fun sessionOf(
        vararg exercises: Pair<Exercise, List<LoggedSet>>,
    ) = SessionWithContent(
        session = WorkoutSession(id = 1, startedAt = 0, lastActivityAt = 0),
        exercises = exercises.mapIndexed { index, (exercise, sets) ->
            SessionExerciseWithDetails(
                sessionExercise = SessionExercise(
                    id = index + 1L,
                    sessionId = 1,
                    exerciseId = exercise.id,
                ),
                exercise = exercise,
                sets = sets,
            )
        },
    )

    private fun set(load: Double, reps: Int, completed: Boolean = true) =
        LoggedSet(sessionExerciseId = 1, load = load, reps = reps, completed = completed)

    @Test
    fun `session volume adds up completed sets across exercises`() {
        val content = sessionOf(
            exercise(1, MeasurementType.WEIGHT_KG) to listOf(set(100.0, 5), set(100.0, 3)),
            exercise(2, MeasurementType.WEIGHT_KG) to listOf(set(50.0, 10)),
        )
        // 500 + 300 + 500
        assertEquals(1300.0, VolumeCalculator.sessionVolumeKg(content, 80.0), 0.001)
    }

    @Test
    fun `unfinished sets do not count toward the session total`() {
        val content = sessionOf(
            exercise(1, MeasurementType.WEIGHT_KG) to
                listOf(set(100.0, 5), set(100.0, 5, completed = false)),
        )
        assertEquals(500.0, VolumeCalculator.sessionVolumeKg(content, 80.0), 0.001)
        assertEquals(1, VolumeCalculator.completedSetCount(content))
    }

    @Test
    fun `machine levels contribute nothing but bodyweight work does`() {
        val content = sessionOf(
            exercise(1, MeasurementType.MACHINE_LEVEL) to listOf(set(7.0, 12)),
            exercise(2, MeasurementType.BODYWEIGHT, factor = 65) to listOf(set(10.0, 10)),
        )
        // levels: 0; bodyweight: (80 * 0.65 + 10) * 10 = 620
        assertEquals(620.0, VolumeCalculator.sessionVolumeKg(content, 80.0), 0.001)
        // ...but both sets were still done.
        assertEquals(2, VolumeCalculator.completedSetCount(content))
    }

    @Test
    fun `an empty session totals zero rather than failing`() {
        val content = sessionOf()
        assertEquals(0.0, VolumeCalculator.sessionVolumeKg(content, 80.0), 0.0)
        assertEquals(0, VolumeCalculator.completedSetCount(content))
    }
}
