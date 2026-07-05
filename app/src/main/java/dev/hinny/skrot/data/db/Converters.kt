package dev.hinny.skrot.data.db

import androidx.room.TypeConverter
import dev.hinny.skrot.data.model.MuscleGroup

class Converters {
    @TypeConverter
    fun muscleGroupListToString(value: List<MuscleGroup>): String =
        value.joinToString(",") { it.name }

    @TypeConverter
    fun stringToMuscleGroupList(value: String): List<MuscleGroup> =
        if (value.isBlank()) emptyList()
        else value.split(",").mapNotNull { name -> MuscleGroup.entries.find { it.name == name } }

    @TypeConverter
    fun stringListToString(value: List<String>): String = value.joinToString(SEPARATOR)

    @TypeConverter
    fun stringToStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(SEPARATOR)

    @TypeConverter
    fun intListToString(value: List<Int>): String = value.joinToString(",")

    @TypeConverter
    fun stringToIntList(value: String): List<Int> =
        if (value.isBlank()) emptyList() else value.split(",").mapNotNull { it.toIntOrNull() }

    private companion object {
        /** ASCII unit separator — never appears in user-entered text like tags. */
        const val SEPARATOR = "\u001F"
    }
}
