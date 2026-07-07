package dev.hinny.skrot.data.db

import dev.hinny.skrot.data.model.Equipment
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `equipment list round-trips`() {
        val list = listOf(Equipment.BARBELL, Equipment.BENCH, Equipment.RACK)
        val text = converters.equipmentListToString(list)
        assertEquals(list, converters.stringToEquipmentList(text))
    }

    @Test
    fun `v1 single-value equipment strings parse as one-element lists`() {
        assertEquals(listOf(Equipment.DUMBBELL), converters.stringToEquipmentList("DUMBBELL"))
        assertEquals(emptyList<Equipment>(), converters.stringToEquipmentList(""))
    }

    @Test
    fun `legacy BODYWEIGHT value maps to NONE and unknown names are dropped`() {
        assertEquals(listOf(Equipment.NONE), converters.stringToEquipmentList("BODYWEIGHT"))
        assertEquals(
            listOf(Equipment.CABLE),
            converters.stringToEquipmentList("CABLE,SOMETHING_UNKNOWN"),
        )
    }
}
