package com.codepotro.borno.kiebord.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Vibration
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
import com.codepotro.borno.data.prefs.KiebordPreferences

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
    onUpdateShowGlobeKey: (Boolean) -> Unit = {},
    onUpdateShowEmojiKey: (Boolean) -> Unit = {},
    onUpdateCursorTrackpad: (Boolean) -> Unit = {},
    onUpdateOneHandedMode: (String) -> Unit,
    onUpdateLongPressDelete: (Boolean) -> Unit = {},
    onUpdateSelectiveDeleteMode: (String) -> Unit = {},
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

        SettingsGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggleItem(
                title = "Vibrate on Keypress",
                subtitle = "Subtle tactile feedback on key taps",
                checked = preferences.vibrateOnKeypress,
                position = GroupPosition.TOP,
                onCheckedChange = onUpdateVibrate
            )

            if (preferences.vibrateOnKeypress) {
                // Feedback Type Selector: Haptic vs Vibration
                val colors = LocalSettingsColors.current
                SettingsItemCard(position = GroupPosition.MIDDLE) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isHaptic = preferences.vibrationType == KiebordPreferences.VIBRATION_TYPE_HAPTIC
                        val hapticBg = if (isHaptic) colors.accent.copy(alpha = 0.15f) else colors.inputBackground
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
                        val vibrationBg = if (isVibration) colors.accent.copy(alpha = 0.15f) else colors.inputBackground
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
                        value = strength.toFloat(),
                        valueRange = 10f..100f,
                        steps = 8,
                        valueLabel = strengthLabel,
                        position = GroupPosition.MIDDLE,
                        onValueChange = { onUpdateVibrationStrength(it.toInt()) }
                    )
                }
            }

            SettingsToggleItem(
                title = "Sound on Keypress",
                subtitle = "Audible click feedback when typing",
                checked = preferences.soundOnKeypress,
                position = GroupPosition.BOTTOM,
                onCheckedChange = onUpdateSound
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategoryHeader(title = "Key Display & Popups")

        SettingsGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggleItem(
                title = "Dedicated Number Row",
                subtitle = "Display persistent 5th row of digits above letter keys",
                checked = preferences.showNumberRow,
                position = GroupPosition.TOP,
                onCheckedChange = onUpdateShowNumberRow
            )

            SettingsToggleItem(
                title = "Show Globe Key",
                subtitle = "Show language switch key beside Space bar",
                checked = preferences.showGlobeKey,
                position = GroupPosition.MIDDLE,
                onCheckedChange = onUpdateShowGlobeKey
            )

            AnimatedVisibility(
                visible = preferences.showGlobeKey,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SettingsToggleItem(
                    title = "Replace with Emoji Key",
                    checked = preferences.showEmojiKey,
                    position = GroupPosition.MIDDLE,
                    onCheckedChange = onUpdateShowEmojiKey
                )
            }

            SettingsToggleItem(
                title = "Secondary Key Hints",
                subtitle = "Show secondary symbol hints on the corner of keycaps",
                checked = preferences.showSecondaryKeys,
                position = GroupPosition.MIDDLE,
                onCheckedChange = onUpdateShowSecondaryKeys
            )

            SettingsToggleItem(
                title = "Popup on Keypress",
                subtitle = "Show circular character preview popup when touching keys",
                checked = preferences.showKeyPopups,
                position = GroupPosition.BOTTOM,
                onCheckedChange = onUpdateShowKeyPopups
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategoryHeader(title = "Key Touch & Timing")

        SettingsGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            val delayMs = preferences.longPressDelayMs
            val delayLabel = "$delayMs ms"

            SettingsSliderItem(
                title = "Long-Press Delay",
                subtitle = "Hold duration required to trigger more-keys and symbols",
                value = delayMs.toFloat(),
                valueRange = 150f..600f,
                steps = 8,
                valueLabel = delayLabel,
                position = GroupPosition.ALONE,
                onValueChange = { onUpdateLongPressDelay(it.toInt()) }
            )
        }
    }
}
