package com.codepotro.borno.data.prefs

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
    fun testBornoRememberSuggestionChoiceDefaultAndMutation() {
        val prefs = KiebordPreferences()
        assertEquals(false, prefs.bornoRememberSuggestionChoice)

        val enabledPrefs = prefs.copy(bornoRememberSuggestionChoice = true)
        assertEquals(true, enabledPrefs.bornoRememberSuggestionChoice)
    }

    @Test
    fun testFastDeleteEnabledDefaultAndMutation() {
        val prefs = KiebordPreferences()
        assertEquals(false, prefs.fastDeleteEnabled)

        val enabledPrefs = prefs.copy(fastDeleteEnabled = true)
        assertEquals(true, enabledPrefs.fastDeleteEnabled)
    }

    @Test
    fun testShowGlobeKeyDefaultAndMutation() {
        val prefs = KiebordPreferences()
        assertEquals(true, prefs.showGlobeKey)

        val disabledPrefs = prefs.copy(showGlobeKey = false)
        assertEquals(false, disabledPrefs.showGlobeKey)
    }

    @Test
    fun testShowEmojiKeyDefaultAndMutation() {
        val prefs = KiebordPreferences()
        assertEquals(false, prefs.showEmojiKey)

        val enabledPrefs = prefs.copy(showEmojiKey = true)
        assertEquals(true, enabledPrefs.showEmojiKey)
    }

    @Test
    fun testAutoDeleteRecentClipsDefaultsAndMutation() {
        val prefs = KiebordPreferences()
        assertEquals(false, prefs.autoDeleteRecentClips)
        assertEquals(24, prefs.autoDeleteRecentClipsDurationHours)

        val updated = prefs.copy(autoDeleteRecentClips = true, autoDeleteRecentClipsDurationHours = 12)
        assertEquals(true, updated.autoDeleteRecentClips)
        assertEquals(12, updated.autoDeleteRecentClipsDurationHours)
    }

    @Test
    fun testAutoSpaceAfterSuggestionPerLanguage() {
        val prefs = KiebordPreferences(autoSpaceAfterSuggestion = true)
        assertEquals(true, prefs.isAutoSpaceAfterSuggestionEnabled("en"))
        assertEquals(true, prefs.isAutoSpaceAfterSuggestionEnabled("bn"))
        assertEquals(true, prefs.isAutoSpaceAfterSuggestionEnabled("tr"))

        val bnDisabled = prefs.copy(autoSpaceAfterSuggestionDisabledLanguages = setOf("bn"))
        assertEquals(true, bnDisabled.isAutoSpaceAfterSuggestionEnabled("en"))
        assertEquals(false, bnDisabled.isAutoSpaceAfterSuggestionEnabled("bn"))
        assertEquals(false, bnDisabled.isAutoSpaceAfterSuggestionEnabled("bn_borno"))
        assertEquals(true, bnDisabled.isAutoSpaceAfterSuggestionEnabled("tr"))

        val masterDisabled = bnDisabled.copy(autoSpaceAfterSuggestion = false)
        assertEquals(false, masterDisabled.isAutoSpaceAfterSuggestionEnabled("en"))
        assertEquals(false, masterDisabled.isAutoSpaceAfterSuggestionEnabled("bn"))
        assertEquals(false, masterDisabled.isAutoSpaceAfterSuggestionEnabled("tr"))
        assertEquals(true, masterDisabled.isAutoSpaceAfterSuggestionLanguageEnabled("en"))
        assertEquals(false, masterDisabled.isAutoSpaceAfterSuggestionLanguageEnabled("bn"))
    }

    @Test
    fun testSelectiveDeleteEnabledDefaultAndMutation() {
        val prefs = KiebordPreferences()
        assertEquals(true, prefs.selectiveDeleteEnabled)

        val disabledPrefs = prefs.copy(selectiveDeleteEnabled = false)
        assertEquals(false, disabledPrefs.selectiveDeleteEnabled)
    }

    @Test
    fun testSpacebarSwipeModeDefaultsAndMutation() {
        val prefs = KiebordPreferences()
        assertEquals(KiebordPreferences.SPACEBAR_MODE_SWIPE_LAYOUT_HOLD_CURSOR, prefs.spacebarSwipeMode)

        val updated = prefs.copy(spacebarSwipeMode = KiebordPreferences.SPACEBAR_MODE_SWIPE_CURSOR_HOLD_LAYOUT)
        assertEquals(KiebordPreferences.SPACEBAR_MODE_SWIPE_CURSOR_HOLD_LAYOUT, updated.spacebarSwipeMode)
    }

    @Test
    fun testVolumeKeyGesturesDefaultsAndMutation() {
        val prefs = KiebordPreferences()
        assertEquals(false, prefs.volumeKeyGesturesEnabled)
        assertEquals(KiebordPreferences.VOLUME_ACTION_MOVE_CURSOR, prefs.volumeKeyGestureAction)

        val enabled = prefs.copy(
            volumeKeyGesturesEnabled = true,
            volumeKeyGestureAction = KiebordPreferences.VOLUME_ACTION_SWITCH_LAYOUT
        )
        assertEquals(true, enabled.volumeKeyGesturesEnabled)
        assertEquals(KiebordPreferences.VOLUME_ACTION_SWITCH_LAYOUT, enabled.volumeKeyGestureAction)
    }

    @Test
    fun testFloatingKeyboardPreferencesDefaultsAndMutation() {
        val prefs = KiebordPreferences()
        assertEquals(false, prefs.isFloatingMode)
        assertEquals(320.0f, prefs.floatingWidthPortraitDp, 0.01f)
        assertEquals(420.0f, prefs.floatingWidthLandscapeDp, 0.01f)
        assertEquals(1.0f, prefs.floatingHeightScalePortrait, 0.01f)
        assertEquals(0.95f, prefs.floatingHeightScaleLandscape, 0.01f)
        assertEquals(0.5f, prefs.floatingOffsetRatioXPortrait, 0.01f)
        assertEquals(0.7f, prefs.floatingOffsetRatioYPortrait, 0.01f)

        val updated = prefs.copy(
            isFloatingMode = true,
            floatingWidthPortraitDp = 300.0f,
            floatingHeightScalePortrait = 0.85f,
            floatingOffsetRatioXPortrait = 0.4f,
            floatingOffsetRatioYPortrait = 0.6f
        )
        assertEquals(true, updated.isFloatingMode)
        assertEquals(300.0f, updated.floatingWidthPortraitDp, 0.01f)
        assertEquals(0.85f, updated.floatingHeightScalePortrait, 0.01f)
        assertEquals(0.4f, updated.floatingOffsetRatioXPortrait, 0.01f)
        assertEquals(0.6f, updated.floatingOffsetRatioYPortrait, 0.01f)
    }
}
