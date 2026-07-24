package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.Equipment
import dev.hinny.skrot.data.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseGuesserTest {

    @Test
    fun `guesses muscle group from common exercise names`() {
        assertEquals(MuscleGroup.CHEST, ExerciseGuesser.guessMuscleGroup("Bench Press"))
        assertEquals(MuscleGroup.BACK, ExerciseGuesser.guessMuscleGroup("Lat Pulldown"))
        assertEquals(MuscleGroup.BACK, ExerciseGuesser.guessMuscleGroup("Barbell Row"))
        assertEquals(MuscleGroup.BICEPS, ExerciseGuesser.guessMuscleGroup("Bicep Curl"))
        assertEquals(MuscleGroup.QUADS, ExerciseGuesser.guessMuscleGroup("Barbell Squat"))
        assertEquals(MuscleGroup.HAMSTRINGS, ExerciseGuesser.guessMuscleGroup("Leg Curl Machine"))
        assertEquals(MuscleGroup.HAMSTRINGS, ExerciseGuesser.guessMuscleGroup("Romanian Deadlift"))
        assertEquals(MuscleGroup.BACK, ExerciseGuesser.guessMuscleGroup("Deadlift"))
        assertEquals(MuscleGroup.SHOULDERS, ExerciseGuesser.guessMuscleGroup("Overhead Press"))
        assertEquals(MuscleGroup.ABS, ExerciseGuesser.guessMuscleGroup("Cable Crunch"))
        assertEquals(MuscleGroup.CALVES, ExerciseGuesser.guessMuscleGroup("Standing Calf Raise"))
        assertEquals(MuscleGroup.GLUTES, ExerciseGuesser.guessMuscleGroup("Hip Thrust"))
        assertEquals(MuscleGroup.FULL_BODY, ExerciseGuesser.guessMuscleGroup("Some Unknown Move"))
    }

    @Test
    fun `guesses equipment from keywords in the name`() {
        assertEquals(
            listOf(Equipment.BARBELL, Equipment.BENCH),
            ExerciseGuesser.guessEquipment("Barbell Bench Press"),
        )
        assertEquals(listOf(Equipment.DUMBBELL), ExerciseGuesser.guessEquipment("Dumbbell Curl"))
        assertEquals(listOf(Equipment.CABLE), ExerciseGuesser.guessEquipment("Cable Row"))
        assertEquals(listOf(Equipment.MACHINE), ExerciseGuesser.guessEquipment("Leg Press Machine"))
        assertEquals(listOf(Equipment.PULLUP_BAR), ExerciseGuesser.guessEquipment("Pull-Up"))
    }

    @Test
    fun `falls back to bodyweight equipment for names with no equipment keyword`() {
        assertEquals(listOf(Equipment.NONE), ExerciseGuesser.guessEquipment("Push-Up"))
        assertEquals(listOf(Equipment.NONE), ExerciseGuesser.guessEquipment("Sit-Up"))
        assertTrue(ExerciseGuesser.guessEquipment("Some Unknown Move").isEmpty())
    }
}
