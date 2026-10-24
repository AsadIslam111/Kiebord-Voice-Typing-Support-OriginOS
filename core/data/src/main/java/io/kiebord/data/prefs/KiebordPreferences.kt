package io.kiebord.data.prefs

/**
 * Immutable snapshot of all user-configurable settings and preferences for Kiebord.
 */
data class KiebordPreferences(
    // Appearance & Theming
    val theme: String = THEME_DEFAULT_DARK,
    val keyboardHeightScale: Float = 1.0f,
    val showNumberRow: Boolean = false,
    val showKeyBorders: Boolean = true,
    val showKeyPopups: Boolean = true,
    val showSecondaryKeys: Boolean = true,

    // Preferences & Haptics
    val vibrateOnKeypress: Boolean = true,
    val vibrationType: String = VIBRATION_TYPE_HAPTIC,
    val vibrationStrength: Int = 50, // 0 - 100%
    val soundOnKeypress: Boolean = false,
    val longPressDelayMs: Int = 300, // 150 - 600 ms
    val cursorTrackpadEnabled: Boolean = true,
    val oneHandedMode: String = ONE_HANDED_OFF,
    val longPressDeleteEnabled: Boolean = true,
    val selectiveDeleteMode: String = DELETE_MODE_WORD,

    // Keyboard Layouts
    val primaryLayoutId: String = LAYOUT_EN_QWERTY,
    val enabledLayoutIds: Set<String> = setOf(
        LAYOUT_EN_QWERTY,
        LAYOUT_BN_BORNO,
        LAYOUT_BN_PROBHAT
    ),

    // Corrections & Suggestions
    val suggestionsEnabled: Boolean = true,
    val nextWordPrediction: Boolean = true,
    val bengaliSmartPhonetic: Boolean = true,
    val learnedWordsEnabled: Boolean = true,
    val gestureTypingEnabled: Boolean = true,
    val showGestureTrail: Boolean = true,
    val autoCapitalization: Boolean = true,
    val doubleSpacePeriod: Boolean = true,

    // Toolbar rearrangement persistence
    val pinnedTools: List<String> = listOf("TEXT_EDIT", "CLIPBOARD", "THEME_TOGGLE", "RESIZER"),
    val drawerTools: List<String> = listOf("NUMBER_ROW_TOGGLE", "SECONDARY_KEYS_TOGGLE", "EMOJI", "ONE_HANDED", "SETTINGS")
) {
    companion object {
        const val THEME_DEFAULT_DARK = "default_dark"
        const val THEME_AMOLED_BLACK = "amoled_black"
        const val THEME_TOKYO_NIGHT = "tokyo_night"
        const val THEME_NORD = "nord"
        const val THEME_DEFAULT_LIGHT = "default_light"

        const val ONE_HANDED_OFF = "off"
        const val ONE_HANDED_LEFT = "left"
        const val ONE_HANDED_RIGHT = "right"

        const val DELETE_MODE_WORD = "word"
        const val DELETE_MODE_CHARACTER = "character"

        const val VIBRATION_TYPE_HAPTIC = "haptic"
        const val VIBRATION_TYPE_VIBRATION = "vibration"

        const val LAYOUT_EN_QWERTY = "en_qwerty"
        const val LAYOUT_BN_BORNO = "bn_borno"
        const val LAYOUT_BN_PROBHAT = "bn_probhat"
    }
}
