package io.kiebord.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Test

class KiebordPreferencesTest {

    @Test
    fun testSuggestionBarModeDefaultsAndConstants() {
        val prefs = KiebordPreferences()
        assertEquals(KiebordPreferences.SUGGESTION_BAR_MODE_NORMAL, prefs.suggestionBarMode)
        assertEquals("normal", KiebordPreferences.SUGGESTION_BAR_MODE_NORMAL)
        assertEquals("scrollable", KiebordPreferences.SUGGESTION_BAR_MODE_SCROLLABLE)

        val scrollablePrefs = prefs.copy(suggestionBarMode = KiebordPreferences.SUGGESTION_BAR_MODE_SCROLLABLE)
        assertEquals(KiebordPreferences.SUGGESTION_BAR_MODE_SCROLLABLE, scrollablePrefs.suggestionBarMode)
    }
}
