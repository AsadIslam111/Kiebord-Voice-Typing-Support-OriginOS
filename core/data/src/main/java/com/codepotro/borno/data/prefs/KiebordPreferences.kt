package com.codepotro.borno.data.prefs

/**
 * Immutable snapshot of all user-configurable settings and preferences for Kiebord.
 */
data class KiebordPreferences(
    // Appearance & Theming
    val theme: String = THEME_DEFAULT_DARK,
    val keyboardHeightScale: Float = 1.0f,
    val keyboardHeightScaleLandscape: Float = 1.0f,
    val chinPaddingPortraitDp: Float = 22.0f,
    val chinPaddingLandscapeDp: Float = 8.0f,
    val horizontalMarginPortraitDp: Float = 0.0f,
    val horizontalMarginLandscapeDp: Float = 0.0f,
    val showNumberRow: Boolean = false,
    val showKeyBorders: Boolean = true,
    val showKeyShadows: Boolean = true,
    val showKeyPopups: Boolean = true,
    val showSecondaryKeys: Boolean = true,
    val showGlobeKey: Boolean = true,
    val showEmojiKey: Boolean = false,
    val showEmojiRow: Boolean = false,
    val recentEmojis: List<String> = emptyList(),

    // Preferences & Haptics
    val vibrateOnKeypress: Boolean = true,
    val vibrationType: String = VIBRATION_TYPE_HAPTIC,
    val vibrationStrength: Int = 50, // 0 - 100%
    val soundOnKeypress: Boolean = false,
    val soundVolume: Int = 100, // 1 - 100%
    val longPressDelayMs: Int = 300, // 150 - 600 ms
    val cursorTrackpadEnabled: Boolean = true,
    val spacebarSwipeMode: String = SPACEBAR_MODE_SWIPE_LAYOUT_HOLD_CURSOR,
    val oneHandedMode: String = ONE_HANDED_OFF,
    val isFloatingMode: Boolean = false,
    val floatingWidthPortraitDp: Float = 320.0f,
    val floatingWidthLandscapeDp: Float = 420.0f,
    val floatingHeightScalePortrait: Float = 1.0f,
    val floatingHeightScaleLandscape: Float = 0.95f,
    val floatingOffsetRatioXPortrait: Float = 0.5f,
    val floatingOffsetRatioYPortrait: Float = 0.7f,
    val floatingOffsetRatioXLandscape: Float = 0.5f,
    val floatingOffsetRatioYLandscape: Float = 0.7f,
    val longPressDeleteEnabled: Boolean = true,
    val selectiveDeleteEnabled: Boolean = true,
    val selectiveDeleteMode: String = DELETE_MODE_WORD,
    val fastDeleteEnabled: Boolean = false,
    val volumeKeyGesturesEnabled: Boolean = false,
    val volumeKeyGestureAction: String = VOLUME_ACTION_MOVE_CURSOR,

    // Keyboard Layouts
    val primaryLayoutId: String = LAYOUT_EN_QWERTY,
    val enabledLayoutIds: Set<String> = linkedSetOf(
        LAYOUT_EN_QWERTY,
        LAYOUT_BN_BORNO,
        LAYOUT_BN_NATIONAL
    ),

    // Corrections & Suggestions
    val suggestionsEnabled: Boolean = true,
    val nextWordPrediction: Boolean = true,
    val phoneticAutoCorrect: Boolean = true,
    val bengaliSmartPhonetic: Boolean = true,
    val bengaliShowRawInputCandidate: Boolean = true,
    val bornoRememberSuggestionChoice: Boolean = false,
    val learnedWordsEnabled: Boolean = true,
    val gestureTypingEnabled: Boolean = true,
    val showGestureTrail: Boolean = true,
    val autoCapitalization: Boolean = true,
    val doubleSpacePeriod: Boolean = true,
    val autoSpaceAfterSuggestion: Boolean = true,
    val autoSpaceAfterSuggestionDisabledLanguages: Set<String> = emptySet(),
    val autoSpaceAfterPunctuation: Boolean = true,
    val removeSpaceBeforePunctuation: Boolean = true,
    val traditionalMode: Boolean = false,
    val suggestionBarMode: String = SUGGESTION_BAR_MODE_NORMAL,
    val emojiSuggestions: Boolean = true,

    // Toolbar rearrangement persistence
    val pinnedTools: List<String> = listOf("EMOJI", "TEXT_EDIT", "NUMBER_PAD", "CLIPBOARD", "THEME_TOGGLE"),
    val drawerTools: List<String> = listOf("RESIZER", "FLOATING", "NUMBER_ROW_TOGGLE", "SECONDARY_KEYS_TOGGLE", "KEY_POPUP_TOGGLE", "ONE_HANDED", "SETTINGS", "TRADITIONAL_MODE", "UNDO", "REDO", "EMOJI_ROW_TOGGLE"),

    // Voice Typing
    val voiceTypingEnabled: Boolean = true,
    val voiceTypingProvider: String = VOICE_PROVIDER_GOOGLE,

    val autoDeleteRecentClips: Boolean = false,
    val autoDeleteRecentClipsDurationHours: Int = 24,

    // Layout-specific dynamic extra row overrides ("${layoutId}_${rowId}" -> Boolean)
    val extraRowOverrides: Map<String, Boolean> = emptyMap(),

    // Onboarding / Setup status
    val setupCompleted: Boolean = false
) {
    fun isExtraRowEnabled(layoutId: String, rowId: String, default: Boolean = true): Boolean {
        return extraRowOverrides["${layoutId}_${rowId}"] ?: default
    }

    fun isAutoSpaceAfterSuggestionLanguageEnabled(languageCode: String): Boolean {
        val lang = when {
            languageCode.startsWith("bn") -> "bn"
            languageCode.startsWith("en") -> "en"
            else -> languageCode.lowercase().trim()
        }
        return lang !in autoSpaceAfterSuggestionDisabledLanguages
    }

    fun isAutoSpaceAfterSuggestionEnabled(languageCode: String): Boolean {
        if (!autoSpaceAfterSuggestion) return false
        return isAutoSpaceAfterSuggestionLanguageEnabled(languageCode)
    }

    companion object {
        const val VOICE_PROVIDER_GOOGLE = "google"

        const val THEME_DEFAULT_DARK = "default_dark"
        const val THEME_AMOLED_BLACK = "amoled_black"
        const val THEME_TOKYO_NIGHT = "tokyo_night"
        const val THEME_NORD = "nord"
        const val THEME_DEFAULT_LIGHT = "default_light"
        const val THEME_ADAPTIVE = "dynamic_adaptive"
        const val THEME_ADAPTIVE_STROKED = "dynamic_adaptive_stroked"

        fun isAdaptiveTheme(themeId: String): Boolean {
            return themeId == THEME_ADAPTIVE || themeId == THEME_ADAPTIVE_STROKED || themeId == "dynamic_adaptive_light_keys"
        }

        const val ONE_HANDED_OFF = "off"
        const val ONE_HANDED_LEFT = "left"
        const val ONE_HANDED_RIGHT = "right"

        const val DELETE_MODE_WORD = "word"
        const val DELETE_MODE_CHARACTER = "character"

        const val SPACEBAR_MODE_SWIPE_LAYOUT_HOLD_CURSOR = "swipe_layout_hold_cursor"
        const val SPACEBAR_MODE_SWIPE_CURSOR_HOLD_LAYOUT = "swipe_cursor_hold_layout"

        const val VOLUME_ACTION_MOVE_CURSOR = "move_cursor"
        const val VOLUME_ACTION_SWITCH_LAYOUT = "switch_layout"

        const val VIBRATION_TYPE_HAPTIC = "haptic"
        const val VIBRATION_TYPE_VIBRATION = "vibration"

        const val SUGGESTION_BAR_MODE_NORMAL = "normal"
        const val SUGGESTION_BAR_MODE_SCROLLABLE = "scrollable"

        const val LAYOUT_EN_QWERTY = "en_qwerty"
        const val LAYOUT_BN_BORNO = "bn_borno"
        const val LAYOUT_BN_AVRO = "bn_avro"
        const val LAYOUT_BN_PROBHAT = "bn_probhat"
        const val LAYOUT_BN_NATIONAL = "bn_national"
        const val LAYOUT_BN_EASY = "bn_easy"
        const val LAYOUT_AR_ARABIC = "ar_arabic"
    }
}
