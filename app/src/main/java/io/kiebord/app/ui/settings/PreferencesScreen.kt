package io.kiebord.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.ViewCompact
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kiebord.data.prefs.KiebordPreferences

@Composable
fun PreferencesScreen(
    preferences: KiebordPreferences,
    onUpdateVibrate: (Boolean) -> Unit,
    onUpdateVibrationStrength: (Int) -> Unit,
    onUpdateSound: (Boolean) -> Unit,
    onUpdateLongPressDelay: (Int) -> Unit,
    onUpdateCursorTrackpad: (Boolean) -> Unit,
    onUpdateOneHandedMode: (String) -> Unit,
    onUpdateLongPressDelete: (Boolean) -> Unit,
    onUpdateSelectiveDeleteMode: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        SettingsCategoryHeader(title = "Haptic & Acoustic Feedback")

        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggleItem(
                title = "Vibrate on Keypress",
                subtitle = "Subtle tactile haptic feedback on key taps",
                icon = Icons.Outlined.Vibration,
                checked = preferences.vibrateOnKeypress,
                onCheckedChange = onUpdateVibrate
            )

            if (preferences.vibrateOnKeypress) {
                val strength = preferences.vibrationStrength
                val strengthLabel = when {
                    strength <= 25 -> "Light ($strength%)"
                    strength <= 65 -> "Medium ($strength%)"
                    else -> "Strong ($strength%)"
                }

                SettingsSliderItem(
                    title = "Vibration Intensity",
                    subtitle = "Haptic motor pulse amplitude",
                    icon = Icons.Outlined.Speed,
                    value = strength.toFloat(),
                    valueRange = 10f..100f,
                    steps = 8,
                    valueLabel = strengthLabel,
                    onValueChange = { onUpdateVibrationStrength(it.toInt()) }
                )
            }

            SettingsToggleItem(
                title = "Sound on Keypress",
                subtitle = "Audible click feedback when typing",
                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                checked = preferences.soundOnKeypress,
                showDivider = false,
                onCheckedChange = onUpdateSound
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategoryHeader(title = "Key Touch & Timing")

        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            val delayMs = preferences.longPressDelayMs
            val delayLabel = "$delayMs ms"

            SettingsSliderItem(
                title = "Long-Press Delay",
                subtitle = "Hold duration required to trigger more-keys and symbols",
                icon = Icons.Outlined.HourglassBottom,
                value = delayMs.toFloat(),
                valueRange = 150f..600f,
                steps = 8,
                valueLabel = delayLabel,
                showDivider = false,
                onValueChange = { onUpdateLongPressDelay(it.toInt()) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategoryHeader(title = "Smart Gestures")

        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggleItem(
                title = "Long-Press Continuous Delete",
                subtitle = "Holding backspace repeats deletion smoothly",
                icon = Icons.AutoMirrored.Outlined.Backspace,
                checked = preferences.longPressDeleteEnabled,
                onCheckedChange = onUpdateLongPressDelete
            )

            if (preferences.longPressDeleteEnabled) {
                SettingsRadioOption(
                    title = "Selective Delete: By Word",
                    subtitle = "Repeated deletion deletes word-by-word at whitespace boundaries",
                    selected = preferences.selectiveDeleteMode == KiebordPreferences.DELETE_MODE_WORD,
                    onSelect = { onUpdateSelectiveDeleteMode(KiebordPreferences.DELETE_MODE_WORD) }
                )

                SettingsRadioOption(
                    title = "Selective Delete: By Character",
                    subtitle = "Repeated deletion removes one individual character at a time",
                    selected = preferences.selectiveDeleteMode == KiebordPreferences.DELETE_MODE_CHARACTER,
                    onSelect = { onUpdateSelectiveDeleteMode(KiebordPreferences.DELETE_MODE_CHARACTER) }
                )
            }

            SettingsToggleItem(
                title = "Spacebar Cursor Trackpad",
                subtitle = "Slide thumb horizontally across spacebar to position cursor",
                icon = Icons.Outlined.TouchApp,
                checked = preferences.cursorTrackpadEnabled,
                showDivider = false,
                onCheckedChange = onUpdateCursorTrackpad
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategoryHeader(title = "Layout Alignment & Ergonomics")

        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsRadioOption(
                title = "Standard Centered (Full Width)",
                subtitle = "Full display width keyboard",
                selected = preferences.oneHandedMode == KiebordPreferences.ONE_HANDED_OFF,
                onSelect = { onUpdateOneHandedMode(KiebordPreferences.ONE_HANDED_OFF) }
            )

            SettingsRadioOption(
                title = "One-Handed (Left Hand)",
                subtitle = "Compact layout aligned toward left edge",
                selected = preferences.oneHandedMode == KiebordPreferences.ONE_HANDED_LEFT,
                onSelect = { onUpdateOneHandedMode(KiebordPreferences.ONE_HANDED_LEFT) }
            )

            SettingsRadioOption(
                title = "One-Handed (Right Hand)",
                subtitle = "Compact layout aligned toward right edge",
                selected = preferences.oneHandedMode == KiebordPreferences.ONE_HANDED_RIGHT,
                showDivider = false,
                onSelect = { onUpdateOneHandedMode(KiebordPreferences.ONE_HANDED_RIGHT) }
            )
        }
    }
}
