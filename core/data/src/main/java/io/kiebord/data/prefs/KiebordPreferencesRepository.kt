package io.kiebord.data.prefs

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.kiebordDataStore: DataStore<Preferences> by preferencesDataStore(name = "kiebord_settings")

class KiebordPreferencesRepository private constructor(private val context: Context) {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("pref_theme")
        val KEYBOARD_HEIGHT_SCALE = floatPreferencesKey("pref_keyboard_height_scale")
        val KEYBOARD_HEIGHT_SCALE_LANDSCAPE = floatPreferencesKey("pref_keyboard_height_scale_landscape")
        val CHIN_PADDING_PORTRAIT = floatPreferencesKey("pref_chin_padding_portrait")
        val CHIN_PADDING_LANDSCAPE = floatPreferencesKey("pref_chin_padding_landscape")
        val HORIZONTAL_MARGIN_PORTRAIT = floatPreferencesKey("pref_horizontal_margin_portrait")
        val HORIZONTAL_MARGIN_LANDSCAPE = floatPreferencesKey("pref_horizontal_margin_landscape")
        val SHOW_NUMBER_ROW = booleanPreferencesKey("pref_show_number_row")
        val SHOW_KEY_BORDERS = booleanPreferencesKey("pref_show_key_borders")
        val SHOW_KEY_POPUPS = booleanPreferencesKey("pref_show_key_popups")
        val SHOW_SECONDARY_KEYS = booleanPreferencesKey("pref_show_secondary_keys")

        val VIBRATE_ON_KEYPRESS = booleanPreferencesKey("pref_vibrate_on_keypress")
        val VIBRATION_TYPE = stringPreferencesKey("pref_vibration_type")
        val VIBRATION_STRENGTH = intPreferencesKey("pref_vibration_strength")
        val SOUND_ON_KEYPRESS = booleanPreferencesKey("pref_sound_on_keypress")
        val LONG_PRESS_DELAY_MS = intPreferencesKey("pref_long_press_delay_ms")
        val CURSOR_TRACKPAD_ENABLED = booleanPreferencesKey("pref_cursor_trackpad_enabled")
        val ONE_HANDED_MODE = stringPreferencesKey("pref_one_handed_mode")
        val LONG_PRESS_DELETE_ENABLED = booleanPreferencesKey("pref_long_press_delete_enabled")
        val SELECTIVE_DELETE_MODE = stringPreferencesKey("pref_selective_delete_mode")

        val PRIMARY_LAYOUT_ID = stringPreferencesKey("pref_primary_layout_id")
        val ENABLED_LAYOUT_IDS = stringSetPreferencesKey("pref_enabled_layout_ids")

        val SUGGESTIONS_ENABLED = booleanPreferencesKey("pref_suggestions_enabled")
        val NEXT_WORD_PREDICTION = booleanPreferencesKey("pref_next_word_prediction")
        val BENGALI_SMART_PHONETIC = booleanPreferencesKey("pref_bengali_smart_phonetic")
        val BENGALI_SHOW_RAW_INPUT_CANDIDATE = booleanPreferencesKey("pref_bengali_show_raw_input_candidate")
        val LEARNED_WORDS_ENABLED = booleanPreferencesKey("pref_learned_words_enabled")
        val GESTURE_TYPING_ENABLED = booleanPreferencesKey("pref_gesture_typing_enabled")
        val SHOW_GESTURE_TRAIL = booleanPreferencesKey("pref_show_gesture_trail")
        val AUTO_CAPITALIZATION = booleanPreferencesKey("pref_auto_capitalization")
        val DOUBLE_SPACE_PERIOD = booleanPreferencesKey("pref_double_space_period")
        val AUTO_SPACE_AFTER_SUGGESTION = booleanPreferencesKey("pref_auto_space_after_suggestion")
        val AUTO_SPACE_AFTER_PUNCTUATION = booleanPreferencesKey("pref_auto_space_after_punctuation")
        val REMOVE_SPACE_BEFORE_PUNCTUATION = booleanPreferencesKey("pref_remove_space_before_punctuation")
        val TRADITIONAL_MODE = booleanPreferencesKey("pref_traditional_mode")
        val SUGGESTION_BAR_MODE = stringPreferencesKey("pref_suggestion_bar_mode")

        val PINNED_TOOLS = stringPreferencesKey("pref_pinned_tools")
        val DRAWER_TOOLS = stringPreferencesKey("pref_drawer_tools")
        val VOICE_TYPING_ENABLED = booleanPreferencesKey("pref_voice_typing_enabled")
        val VOICE_TYPING_PROVIDER = stringPreferencesKey("pref_voice_typing_provider")
        val SETUP_COMPLETED = booleanPreferencesKey("pref_setup_completed")
    }

    val preferencesFlow: Flow<KiebordPreferences> = context.kiebordDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            KiebordPreferences(
                theme = prefs[PreferencesKeys.THEME] ?: KiebordPreferences.THEME_DEFAULT_DARK,
                keyboardHeightScale = prefs[PreferencesKeys.KEYBOARD_HEIGHT_SCALE] ?: 1.0f,
                keyboardHeightScaleLandscape = prefs[PreferencesKeys.KEYBOARD_HEIGHT_SCALE_LANDSCAPE] ?: 1.0f,
                chinPaddingPortraitDp = prefs[PreferencesKeys.CHIN_PADDING_PORTRAIT] ?: 8.0f,
                chinPaddingLandscapeDp = prefs[PreferencesKeys.CHIN_PADDING_LANDSCAPE] ?: 4.0f,
                horizontalMarginPortraitDp = prefs[PreferencesKeys.HORIZONTAL_MARGIN_PORTRAIT] ?: 0.0f,
                horizontalMarginLandscapeDp = prefs[PreferencesKeys.HORIZONTAL_MARGIN_LANDSCAPE] ?: 0.0f,
                showNumberRow = prefs[PreferencesKeys.SHOW_NUMBER_ROW] ?: false,
                showKeyBorders = prefs[PreferencesKeys.SHOW_KEY_BORDERS] ?: true,
                showKeyPopups = prefs[PreferencesKeys.SHOW_KEY_POPUPS] ?: true,
                showSecondaryKeys = prefs[PreferencesKeys.SHOW_SECONDARY_KEYS] ?: true,

                vibrateOnKeypress = prefs[PreferencesKeys.VIBRATE_ON_KEYPRESS] ?: true,
                vibrationType = prefs[PreferencesKeys.VIBRATION_TYPE] ?: KiebordPreferences.VIBRATION_TYPE_HAPTIC,
                vibrationStrength = prefs[PreferencesKeys.VIBRATION_STRENGTH] ?: 50,
                soundOnKeypress = prefs[PreferencesKeys.SOUND_ON_KEYPRESS] ?: false,
                longPressDelayMs = prefs[PreferencesKeys.LONG_PRESS_DELAY_MS] ?: 300,
                cursorTrackpadEnabled = prefs[PreferencesKeys.CURSOR_TRACKPAD_ENABLED] ?: true,
                oneHandedMode = prefs[PreferencesKeys.ONE_HANDED_MODE] ?: KiebordPreferences.ONE_HANDED_OFF,
                longPressDeleteEnabled = prefs[PreferencesKeys.LONG_PRESS_DELETE_ENABLED] ?: true,
                selectiveDeleteMode = prefs[PreferencesKeys.SELECTIVE_DELETE_MODE] ?: KiebordPreferences.DELETE_MODE_WORD,

                primaryLayoutId = prefs[PreferencesKeys.PRIMARY_LAYOUT_ID] ?: KiebordPreferences.LAYOUT_EN_QWERTY,
                enabledLayoutIds = prefs[PreferencesKeys.ENABLED_LAYOUT_IDS] ?: setOf(
                    KiebordPreferences.LAYOUT_EN_QWERTY,
                    KiebordPreferences.LAYOUT_BN_BORNO,
                    KiebordPreferences.LAYOUT_BN_NATIONAL
                ),

                suggestionsEnabled = prefs[PreferencesKeys.SUGGESTIONS_ENABLED] ?: true,
                nextWordPrediction = prefs[PreferencesKeys.NEXT_WORD_PREDICTION] ?: true,
                bengaliSmartPhonetic = prefs[PreferencesKeys.BENGALI_SMART_PHONETIC] ?: true,
                bengaliShowRawInputCandidate = prefs[PreferencesKeys.BENGALI_SHOW_RAW_INPUT_CANDIDATE] ?: true,
                learnedWordsEnabled = prefs[PreferencesKeys.LEARNED_WORDS_ENABLED] ?: true,
                gestureTypingEnabled = prefs[PreferencesKeys.GESTURE_TYPING_ENABLED] ?: true,
                showGestureTrail = prefs[PreferencesKeys.SHOW_GESTURE_TRAIL] ?: true,
                autoCapitalization = prefs[PreferencesKeys.AUTO_CAPITALIZATION] ?: true,
                doubleSpacePeriod = prefs[PreferencesKeys.DOUBLE_SPACE_PERIOD] ?: true,
                autoSpaceAfterSuggestion = prefs[PreferencesKeys.AUTO_SPACE_AFTER_SUGGESTION] ?: true,
                autoSpaceAfterPunctuation = prefs[PreferencesKeys.AUTO_SPACE_AFTER_PUNCTUATION] ?: true,
                removeSpaceBeforePunctuation = prefs[PreferencesKeys.REMOVE_SPACE_BEFORE_PUNCTUATION] ?: true,
                traditionalMode = prefs[PreferencesKeys.TRADITIONAL_MODE] ?: false,
                suggestionBarMode = prefs[PreferencesKeys.SUGGESTION_BAR_MODE] ?: KiebordPreferences.SUGGESTION_BAR_MODE_NORMAL,

                pinnedTools = run {
                    val defaultPinned = listOf("TEXT_EDIT", "CLIPBOARD", "THEME_TOGGLE", "RESIZER")
                    val allActionNames = listOf(
                        "TEXT_EDIT", "CLIPBOARD", "THEME_TOGGLE", "RESIZER",
                        "NUMBER_ROW_TOGGLE", "SECONDARY_KEYS_TOGGLE", "KEY_POPUP_TOGGLE", "EMOJI", "ONE_HANDED", "SETTINGS", "TRADITIONAL_MODE",
                        "UNDO", "REDO"
                    )
                    prefs[PreferencesKeys.PINNED_TOOLS]?.let { raw ->
                        raw.split(",").filter { it in allActionNames }.distinct()
                    } ?: defaultPinned
                },
                drawerTools = run {
                    val defaultPinned = listOf("TEXT_EDIT", "CLIPBOARD", "THEME_TOGGLE", "RESIZER")
                    val defaultDrawer = listOf("NUMBER_ROW_TOGGLE", "SECONDARY_KEYS_TOGGLE", "KEY_POPUP_TOGGLE", "EMOJI", "ONE_HANDED", "SETTINGS", "TRADITIONAL_MODE", "UNDO", "REDO")
                    val allActionNames = listOf(
                        "TEXT_EDIT", "CLIPBOARD", "THEME_TOGGLE", "RESIZER",
                        "NUMBER_ROW_TOGGLE", "SECONDARY_KEYS_TOGGLE", "KEY_POPUP_TOGGLE", "EMOJI", "ONE_HANDED", "SETTINGS", "TRADITIONAL_MODE",
                        "UNDO", "REDO"
                    )
                    val pinnedList = prefs[PreferencesKeys.PINNED_TOOLS]?.let { raw ->
                        raw.split(",").filter { it in allActionNames }.distinct()
                    } ?: defaultPinned
                    val pinnedSet = pinnedList.toSet()
                    val rawDrawer = prefs[PreferencesKeys.DRAWER_TOOLS]?.split(",")?.filter { it in allActionNames } ?: defaultDrawer
                    (rawDrawer.filter { it !in pinnedSet } +
                        allActionNames.filter { it !in pinnedSet }).distinct()
                },
                voiceTypingEnabled = prefs[PreferencesKeys.VOICE_TYPING_ENABLED] ?: true,
                voiceTypingProvider = prefs[PreferencesKeys.VOICE_TYPING_PROVIDER] ?: KiebordPreferences.VOICE_PROVIDER_GOOGLE,
                setupCompleted = prefs[PreferencesKeys.SETUP_COMPLETED] ?: false
            )
        }

    // Appearance & Theming mutators
    suspend fun updateTheme(theme: String) {
        context.kiebordDataStore.edit { it[PreferencesKeys.THEME] = theme }
    }

    suspend fun updateKeyboardHeightScale(scale: Float) {
        context.kiebordDataStore.edit { it[PreferencesKeys.KEYBOARD_HEIGHT_SCALE] = scale }
    }

    suspend fun updateKeyboardHeightScaleLandscape(scale: Float) {
        context.kiebordDataStore.edit { it[PreferencesKeys.KEYBOARD_HEIGHT_SCALE_LANDSCAPE] = scale }
    }

    suspend fun updateKeyboardResizerValues(
        isLandscape: Boolean,
        heightScale: Float,
        chinPaddingDp: Float,
        horizontalMarginDp: Float
    ) {
        context.kiebordDataStore.edit { prefs ->
            if (isLandscape) {
                prefs[PreferencesKeys.KEYBOARD_HEIGHT_SCALE_LANDSCAPE] = heightScale
                prefs[PreferencesKeys.CHIN_PADDING_LANDSCAPE] = chinPaddingDp
                prefs[PreferencesKeys.HORIZONTAL_MARGIN_LANDSCAPE] = horizontalMarginDp
            } else {
                prefs[PreferencesKeys.KEYBOARD_HEIGHT_SCALE] = heightScale
                prefs[PreferencesKeys.CHIN_PADDING_PORTRAIT] = chinPaddingDp
                prefs[PreferencesKeys.HORIZONTAL_MARGIN_PORTRAIT] = horizontalMarginDp
            }
        }
    }

    suspend fun updateShowNumberRow(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.SHOW_NUMBER_ROW] = enabled }
    }

    suspend fun updateShowKeyBorders(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.SHOW_KEY_BORDERS] = enabled }
    }

    suspend fun updateShowKeyPopups(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.SHOW_KEY_POPUPS] = enabled }
    }

    suspend fun updateShowSecondaryKeys(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.SHOW_SECONDARY_KEYS] = enabled }
    }

    // Preferences & Haptics mutators
    suspend fun updateVibrateOnKeypress(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.VIBRATE_ON_KEYPRESS] = enabled }
    }

    suspend fun updateVibrationType(type: String) {
        context.kiebordDataStore.edit { it[PreferencesKeys.VIBRATION_TYPE] = type }
    }

    suspend fun updateVibrationStrength(strength: Int) {
        context.kiebordDataStore.edit { it[PreferencesKeys.VIBRATION_STRENGTH] = strength.coerceIn(0, 100) }
    }

    suspend fun updateSoundOnKeypress(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.SOUND_ON_KEYPRESS] = enabled }
    }

    suspend fun updateLongPressDelayMs(delayMs: Int) {
        context.kiebordDataStore.edit { it[PreferencesKeys.LONG_PRESS_DELAY_MS] = delayMs.coerceIn(150, 600) }
    }

    suspend fun updateCursorTrackpadEnabled(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.CURSOR_TRACKPAD_ENABLED] = enabled }
    }

    suspend fun updateOneHandedMode(mode: String) {
        context.kiebordDataStore.edit { it[PreferencesKeys.ONE_HANDED_MODE] = mode }
    }

    suspend fun updateLongPressDeleteEnabled(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.LONG_PRESS_DELETE_ENABLED] = enabled }
    }

    suspend fun updateSelectiveDeleteMode(mode: String) {
        context.kiebordDataStore.edit { it[PreferencesKeys.SELECTIVE_DELETE_MODE] = mode }
    }

    // Layout mutators
    suspend fun updatePrimaryLayoutId(layoutId: String) {
        context.kiebordDataStore.edit { prefs ->
            prefs[PreferencesKeys.PRIMARY_LAYOUT_ID] = layoutId
            val currentEnabled = prefs[PreferencesKeys.ENABLED_LAYOUT_IDS] ?: setOf(
                KiebordPreferences.LAYOUT_EN_QWERTY,
                KiebordPreferences.LAYOUT_BN_BORNO,
                KiebordPreferences.LAYOUT_BN_NATIONAL
            )
            if (!currentEnabled.contains(layoutId)) {
                prefs[PreferencesKeys.ENABLED_LAYOUT_IDS] = currentEnabled + layoutId
            }
        }
    }

    suspend fun toggleLayoutEnabled(layoutId: String, enabled: Boolean) {
        context.kiebordDataStore.edit { prefs ->
            val currentEnabled = (prefs[PreferencesKeys.ENABLED_LAYOUT_IDS] ?: setOf(
                KiebordPreferences.LAYOUT_EN_QWERTY,
                KiebordPreferences.LAYOUT_BN_BORNO,
                KiebordPreferences.LAYOUT_BN_NATIONAL
            )).toMutableSet()

            if (enabled) {
                currentEnabled.add(layoutId)
            } else {
                // Ensure at least one layout is enabled
                if (currentEnabled.size > 1) {
                    currentEnabled.remove(layoutId)
                    // If primary was removed, point primary to the first available layout
                    val primary = prefs[PreferencesKeys.PRIMARY_LAYOUT_ID]
                    if (primary == layoutId) {
                        prefs[PreferencesKeys.PRIMARY_LAYOUT_ID] = currentEnabled.first()
                    }
                }
            }
            prefs[PreferencesKeys.ENABLED_LAYOUT_IDS] = currentEnabled
        }
    }

    // Suggestions & Corrections mutators
    suspend fun updateSuggestionsEnabled(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.SUGGESTIONS_ENABLED] = enabled }
    }

    suspend fun updateNextWordPrediction(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.NEXT_WORD_PREDICTION] = enabled }
    }

    suspend fun updateBengaliSmartPhonetic(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.BENGALI_SMART_PHONETIC] = enabled }
    }

    suspend fun updateBengaliShowRawInputCandidate(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.BENGALI_SHOW_RAW_INPUT_CANDIDATE] = enabled }
    }

    suspend fun updateLearnedWordsEnabled(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.LEARNED_WORDS_ENABLED] = enabled }
    }

    suspend fun updateGestureTypingEnabled(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.GESTURE_TYPING_ENABLED] = enabled }
    }

    suspend fun updateShowGestureTrail(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.SHOW_GESTURE_TRAIL] = enabled }
    }

    suspend fun updateAutoCapitalization(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.AUTO_CAPITALIZATION] = enabled }
    }

    suspend fun updateDoubleSpacePeriod(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.DOUBLE_SPACE_PERIOD] = enabled }
    }

    suspend fun updateAutoSpaceAfterSuggestion(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.AUTO_SPACE_AFTER_SUGGESTION] = enabled }
    }

    suspend fun updateAutoSpaceAfterPunctuation(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.AUTO_SPACE_AFTER_PUNCTUATION] = enabled }
    }

    suspend fun updateRemoveSpaceBeforePunctuation(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.REMOVE_SPACE_BEFORE_PUNCTUATION] = enabled }
    }

    suspend fun updateTraditionalMode(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.TRADITIONAL_MODE] = enabled }
    }

    suspend fun updateSuggestionBarMode(mode: String) {
        context.kiebordDataStore.edit { it[PreferencesKeys.SUGGESTION_BAR_MODE] = mode }
    }

    suspend fun updateToolbarTools(pinned: List<String>, drawer: List<String>) {
        val defaultPinned = listOf("TEXT_EDIT", "CLIPBOARD", "THEME_TOGGLE", "RESIZER")
        val allActionNames = listOf(
            "TEXT_EDIT", "CLIPBOARD", "THEME_TOGGLE", "RESIZER",
            "NUMBER_ROW_TOGGLE", "SECONDARY_KEYS_TOGGLE", "KEY_POPUP_TOGGLE", "EMOJI", "ONE_HANDED", "SETTINGS", "TRADITIONAL_MODE",
            "UNDO", "REDO"
        )
        val sanitizedPinned = pinned.filter { it in allActionNames }.distinct()
        val pinnedSet = sanitizedPinned.toSet()
        val sanitizedDrawer = (drawer.filter { it in allActionNames && it !in pinnedSet } +
            allActionNames.filter { it !in pinnedSet }).distinct()

        context.kiebordDataStore.edit {
            it[PreferencesKeys.PINNED_TOOLS] = sanitizedPinned.joinToString(",")
            it[PreferencesKeys.DRAWER_TOOLS] = sanitizedDrawer.joinToString(",")
        }
    }

    suspend fun updateVoiceTypingEnabled(enabled: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.VOICE_TYPING_ENABLED] = enabled }
    }

    suspend fun updateVoiceTypingProvider(provider: String) {
        context.kiebordDataStore.edit { it[PreferencesKeys.VOICE_TYPING_PROVIDER] = provider }
    }

    suspend fun updateSetupCompleted(completed: Boolean) {
        context.kiebordDataStore.edit { it[PreferencesKeys.SETUP_COMPLETED] = completed }
    }

    // Reset
    suspend fun resetToDefaults() {
        context.kiebordDataStore.edit { it.clear() }
    }

    companion object {
        @Volatile
        private var INSTANCE: KiebordPreferencesRepository? = null

        fun getInstance(context: Context): KiebordPreferencesRepository {
            val safeContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val app = context.applicationContext ?: context
                if (app.isDeviceProtectedStorage) app else app.createDeviceProtectedStorageContext()
            } else {
                context.applicationContext ?: context
            }
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: KiebordPreferencesRepository(safeContext).also { INSTANCE = it }
            }
        }
    }
}
