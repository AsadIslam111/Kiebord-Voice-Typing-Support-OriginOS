package io.kiebord.data.prefs

import android.content.Context
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
        val SHOW_NUMBER_ROW = booleanPreferencesKey("pref_show_number_row")
        val SHOW_KEY_BORDERS = booleanPreferencesKey("pref_show_key_borders")
        val SHOW_KEY_POPUPS = booleanPreferencesKey("pref_show_key_popups")

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
        val GESTURE_TYPING_ENABLED = booleanPreferencesKey("pref_gesture_typing_enabled")
        val SHOW_GESTURE_TRAIL = booleanPreferencesKey("pref_show_gesture_trail")
        val AUTO_CAPITALIZATION = booleanPreferencesKey("pref_auto_capitalization")
        val DOUBLE_SPACE_PERIOD = booleanPreferencesKey("pref_double_space_period")
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
                showNumberRow = prefs[PreferencesKeys.SHOW_NUMBER_ROW] ?: false,
                showKeyBorders = prefs[PreferencesKeys.SHOW_KEY_BORDERS] ?: true,
                showKeyPopups = prefs[PreferencesKeys.SHOW_KEY_POPUPS] ?: true,

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
                    KiebordPreferences.LAYOUT_BN_PROBHAT
                ),

                suggestionsEnabled = prefs[PreferencesKeys.SUGGESTIONS_ENABLED] ?: true,
                nextWordPrediction = prefs[PreferencesKeys.NEXT_WORD_PREDICTION] ?: true,
                bengaliSmartPhonetic = prefs[PreferencesKeys.BENGALI_SMART_PHONETIC] ?: true,
                gestureTypingEnabled = prefs[PreferencesKeys.GESTURE_TYPING_ENABLED] ?: true,
                showGestureTrail = prefs[PreferencesKeys.SHOW_GESTURE_TRAIL] ?: true,
                autoCapitalization = prefs[PreferencesKeys.AUTO_CAPITALIZATION] ?: true,
                doubleSpacePeriod = prefs[PreferencesKeys.DOUBLE_SPACE_PERIOD] ?: true
            )
        }

    // Appearance & Theming mutators
    suspend fun updateTheme(theme: String) {
        context.kiebordDataStore.edit { it[PreferencesKeys.THEME] = theme }
    }

    suspend fun updateKeyboardHeightScale(scale: Float) {
        context.kiebordDataStore.edit { it[PreferencesKeys.KEYBOARD_HEIGHT_SCALE] = scale }
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
                KiebordPreferences.LAYOUT_BN_PROBHAT
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
                KiebordPreferences.LAYOUT_BN_PROBHAT
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

    // Reset
    suspend fun resetToDefaults() {
        context.kiebordDataStore.edit { it.clear() }
    }

    companion object {
        @Volatile
        private var INSTANCE: KiebordPreferencesRepository? = null

        fun getInstance(context: Context): KiebordPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: KiebordPreferencesRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
