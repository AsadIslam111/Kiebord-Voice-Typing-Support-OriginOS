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

    @Test
    fun testPhoneticAutoCorrectDefault() {
        val prefs = KiebordPreferences()
        assertEquals(true, prefs.phoneticAutoCorrect)

        val disabledPrefs = prefs.copy(phoneticAutoCorrect = false)
        assertEquals(false, disabledPrefs.phoneticAutoCorrect)
    }

    @Test
    fun testFastDeleteEnabledDefaultAndMutation() {
        val prefs = KiebordPreferences()
        assertEquals(false, prefs.fastDeleteEnabled)

        val enabledPrefs = prefs.copy(fastDeleteEnabled = true)
        assertEquals(true, enabledPrefs.fastDeleteEnabled)
    }

    @Test
    fun testShowEmojiKeyDefaultAndMutation() {
        val prefs = KiebordPreferences()
        assertEquals(false, prefs.showEmojiKey)

        val enabledPrefs = prefs.copy(showEmojiKey = true)
        assertEquals(true, enabledPrefs.showEmojiKey)
    }
}
