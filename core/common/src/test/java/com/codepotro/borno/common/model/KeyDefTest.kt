package com.codepotro.borno.common.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyDefTest {

    @Test
    fun flickUp_filtersOutActionTokens() {
        val key = KeyDef(
            label = "ঁ",
            shiftedLabel = "ঃ",
            flickUp = "ঃ",
            shiftedFlickUp = "",
            moreKeys = listOf("ঃ", "ACTION_SELECT_ALL"),
            shiftedMoreKeys = listOf("ACTION_SELECT_ALL")
        )

        assertEquals("ঃ", key.flickUp(ShiftState.UNSHIFTED))
        assertNull(key.flickUp(ShiftState.ONE_SHOT))
        assertNull(key.flickUp(ShiftState.CAPS_LOCK))
    }

    @Test
    fun flickUp_returnsShiftedMoreKeysFirstNonActionWhenShiftedFlickUpNull() {
        val keyWithActionFirst = KeyDef(
            label = "a",
            shiftedLabel = "A",
            shiftedMoreKeys = listOf("ACTION_SELECT_ALL", "Ä")
        )

        assertEquals("Ä", keyWithActionFirst.flickUp(ShiftState.ONE_SHOT))

        val keyWithOnlyAction = KeyDef(
            label = "b",
            shiftedLabel = "B",
            shiftedMoreKeys = listOf("ACTION_SELECT_ALL")
        )

        assertNull(keyWithOnlyAction.flickUp(ShiftState.ONE_SHOT))
    }

    @Test
    fun flickUp_returnsExplicitShiftedFlickUp() {
        val key = KeyDef(
            label = "ক",
            shiftedLabel = "খ",
            flickUp = "খ",
            shiftedFlickUp = "ক"
        )

        assertEquals("খ", key.flickUp(ShiftState.UNSHIFTED))
        assertEquals("ক", key.flickUp(ShiftState.ONE_SHOT))
        assertEquals("ক", key.flickUp(ShiftState.CAPS_LOCK))
    }
}

