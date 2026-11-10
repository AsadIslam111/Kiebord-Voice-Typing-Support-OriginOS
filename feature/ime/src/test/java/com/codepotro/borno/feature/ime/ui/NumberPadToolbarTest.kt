package com.codepotro.borno.feature.ime.ui

import com.codepotro.borno.common.model.KeyDef
import com.codepotro.borno.common.model.KeyType
import com.codepotro.borno.common.model.ShiftState
import com.codepotro.borno.data.layout.PredefinedLayouts
import com.codepotro.borno.data.prefs.KiebordPreferences
import com.codepotro.borno.feature.ime.ui.toolbar.ToolbarAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberPadToolbarTest {

    @Test
    fun testToolbarActionNumberPadExists() {
        assertNotNull(ToolbarAction.NUMBER_PAD)
        assertEquals(ToolbarAction.NUMBER_PAD, ToolbarAction.valueOf("NUMBER_PAD"))
    }

    @Test
    fun testKiebordPreferencesContainsNumberPadInPinnedTools() {
        val prefs = KiebordPreferences()
        assertTrue("pinnedTools must contain NUMBER_PAD by default", prefs.pinnedTools.contains("NUMBER_PAD"))
        assertEquals(
            listOf("EMOJI", "TEXT_EDIT", "NUMBER_PAD", "CLIPBOARD", "THEME_TOGGLE"),
            prefs.pinnedTools
        )
    }

    @Test
    fun testNumberPadEqualAndNotEqualHaveSecondaryKeys() {
        val layout = PredefinedLayouts.NumberPadExtended
        val row0 = layout.rows[0].keys
        val row1 = layout.rows[1].keys

        val notEqualKey = row0.firstOrNull { it.label == "≠" }
        assertNotNull("Not-equal key must exist in row 0", notEqualKey)
        assertEquals(listOf("≈", "≡", "/"), notEqualKey?.moreKeys(ShiftState.UNSHIFTED))

        val equalKey = row1.firstOrNull { it.label == "=" }
        assertNotNull("Equal key must exist in row 1", equalKey)
        assertEquals(listOf("±"), equalKey?.moreKeys(ShiftState.UNSHIFTED))
    }

    @Test
    fun testNumberPadNumericKeysCommitTextMatchesLabel() {
        val layout = PredefinedLayouts.NumberPadExtended
        val numericLabels = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", ".", ",")
        val allKeys = layout.rows.flatMap { it.keys }

        for (label in numericLabels) {
            val key = allKeys.firstOrNull { it.label == label }
            assertNotNull("Key '$label' must exist in NumberPadExtended", key)
            val commit = key?.textToCommit(ShiftState.UNSHIFTED)
            assertEquals("Key '$label' commit text must match its label for press-and-hold fallback", label, commit)
        }
    }
}


