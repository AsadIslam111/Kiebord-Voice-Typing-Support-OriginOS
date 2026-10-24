package io.kiebord.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.SmartButton
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.ViewCompact
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.data.prefs.KiebordPreferences

@Composable
fun PreferencesScreen(
    preferences: KiebordPreferences,
    onUpdateVibrate: (Boolean) -> Unit,
    onUpdateVibrationType: (String) -> Unit = {},
    onUpdateVibrationStrength: (Int) -> Unit,
    onUpdateSound: (Boolean) -> Unit,
    onUpdateLongPressDelay: (Int) -> Unit,
    onUpdateShowNumberRow: (Boolean) -> Unit,
    onUpdateShowKeyPopups: (Boolean) -> Unit,
    onUpdateShowSecondaryKeys: (Boolean) -> Unit = {},
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
                subtitle = "Subtle tactile feedback on key taps",
                icon = Icons.Outlined.Vibration,
                checked = preferences.vibrateOnKeypress,
                onCheckedChange = onUpdateVibrate
            )

            if (preferences.vibrateOnKeypress) {
                // Feedback Type Selector: Haptic vs Vibration
                val colors = LocalSettingsColors.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isHaptic = preferences.vibrationType == KiebordPreferences.VIBRATION_TYPE_HAPTIC
                    val hapticBg = if (isHaptic) (if (colors.isDark) Color(0xFF233047) else Color(0xFFEBF2FE)) else colors.inputBackground
                    val hapticBorder = if (isHaptic) colors.accent else colors.inputBorder
                    val hapticTint = if (isHaptic) colors.accent else colors.textSecondary
                    val hapticText = if (isHaptic) colors.accent else colors.textPrimary

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(hapticBg)
                            .border(1.dp, hapticBorder, RoundedCornerShape(10.dp))
                            .clickable { onUpdateVibrationType(KiebordPreferences.VIBRATION_TYPE_HAPTIC) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.TouchApp,
                                contentDescription = null,
                                tint = hapticTint,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Haptic",
                                fontSize = 13.sp,
                                fontWeight = if (isHaptic) FontWeight.Bold else FontWeight.Medium,
                                color = hapticText
                            )
                        }
                    }

                    val isVibration = preferences.vibrationType == KiebordPreferences.VIBRATION_TYPE_VIBRATION
                    val vibrationBg = if (isVibration) (if (colors.isDark) Color(0xFF233047) else Color(0xFFEBF2FE)) else colors.inputBackground
                    val vibrationBorder = if (isVibration) colors.accent else colors.inputBorder
                    val vibrationTint = if (isVibration) colors.accent else colors.textSecondary
                    val vibrationText = if (isVibration) colors.accent else colors.textPrimary

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(vibrationBg)
                            .border(1.dp, vibrationBorder, RoundedCornerShape(10.dp))
                            .clickable { onUpdateVibrationType(KiebordPreferences.VIBRATION_TYPE_VIBRATION) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Vibration,
                                contentDescription = null,
                                tint = vibrationTint,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Vibration",
                                fontSize = 13.sp,
                                fontWeight = if (isVibration) FontWeight.Bold else FontWeight.Medium,
                                color = vibrationText
                            )
                        }
                    }
                }

                if (preferences.vibrationType == KiebordPreferences.VIBRATION_TYPE_VIBRATION) {
                    val strength = preferences.vibrationStrength
                    val strengthLabel = when {
                        strength <= 25 -> "Light ($strength%)"
                        strength <= 65 -> "Medium ($strength%)"
                        else -> "Strong ($strength%)"
                    }

                    SettingsSliderItem(
                        title = "Vibration Intensity",
                        subtitle = "Motor vibration pulse strength",
                        icon = Icons.Outlined.Speed,
                        value = strength.toFloat(),
                        valueRange = 10f..100f,
                        steps = 8,
                        valueLabel = strengthLabel,
                        onValueChange = { onUpdateVibrationStrength(it.toInt()) }
                    )
                }
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
        SettingsCategoryHeader(title = "Key Display & Popups")

        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggleItem(
                title = "Dedicated Number Row",
                subtitle = "Display persistent 5th row of digits above letter keys",
                icon = Icons.Outlined.Numbers,
                checked = preferences.showNumberRow,
                onCheckedChange = onUpdateShowNumberRow
            )

            SettingsToggleItem(
                title = "Secondary Key Hints",
                subtitle = "Show secondary symbol hints on the corner of keycaps",
                icon = Icons.Outlined.Layers,
                checked = preferences.showSecondaryKeys,
                onCheckedChange = onUpdateShowSecondaryKeys
            )

            SettingsToggleItem(
                title = "Popup on Keypress",
                subtitle = "Show circular character preview popup when touching keys",
                icon = Icons.Outlined.SmartButton,
                checked = preferences.showKeyPopups,
                showDivider = false,
                onCheckedChange = onUpdateShowKeyPopups
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
                title = "Long-Press Repeat Delete",
                subtitle = "Holding backspace repeats deletion continuously like standard keyboard",
                icon = Icons.AutoMirrored.Outlined.Backspace,
                checked = preferences.longPressDeleteEnabled,
                onCheckedChange = onUpdateLongPressDelete
            )

            SettingsRadioOption(
                title = "Swipe Delete (Selective): By Word",
                subtitle = "Swiping left from backspace selects and deletes text word-by-word",
                selected = preferences.selectiveDeleteMode == KiebordPreferences.DELETE_MODE_WORD,
                onSelect = { onUpdateSelectiveDeleteMode(KiebordPreferences.DELETE_MODE_WORD) }
            )

            SettingsRadioOption(
                title = "Swipe Delete (Selective): By Character",
                subtitle = "Swiping left from backspace selects and deletes text character-by-character",
                selected = preferences.selectiveDeleteMode == KiebordPreferences.DELETE_MODE_CHARACTER,
                onSelect = { onUpdateSelectiveDeleteMode(KiebordPreferences.DELETE_MODE_CHARACTER) }
            )

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
