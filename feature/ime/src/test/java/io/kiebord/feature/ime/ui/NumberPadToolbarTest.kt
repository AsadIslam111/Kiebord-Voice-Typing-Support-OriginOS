package io.kiebord.feature.ime.ui

import io.kiebord.data.prefs.KiebordPreferences
import io.kiebord.feature.ime.ui.toolbar.ToolbarAction
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
}

