package io.kiebord.app.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Spellcheck
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.data.prefs.KiebordPreferences

@Composable
fun SettingsMainScreen(
    preferences: KiebordPreferences,
    isImeEnabled: Boolean,
    isImeSelected: Boolean,
    onOpenSettings: () -> Unit,
    onSwitchIme: () -> Unit,
    onNavigate: (SettingsScreen) -> Unit
) {
    val scrollState = rememberScrollState()
    var testText by remember { mutableStateOf("") }

    val themeBadge = when (preferences.theme) {
        KiebordPreferences.THEME_AMOLED_BLACK -> "AMOLED"
        KiebordPreferences.THEME_TOKYO_NIGHT -> "Tokyo Night"
        KiebordPreferences.THEME_NORD -> "Nord"
        KiebordPreferences.THEME_DEFAULT_LIGHT -> "Light"
        else -> "Dark"
    }

    val layoutBadge = when (preferences.primaryLayoutId) {
        KiebordPreferences.LAYOUT_BN_BORNO -> "Borno"
        KiebordPreferences.LAYOUT_BN_PROBHAT -> "Probhat"
        KiebordPreferences.LAYOUT_BN_NATIONAL -> "Jatiyo"
        else -> "QWERTY"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        val colors = LocalSettingsColors.current

        // Activation / Status Card (inspired by SetupStatusPreference)
        if (!isImeEnabled || !isImeSelected) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.warningCardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (!isImeEnabled) "Keyboard Disabled" else "Keyboard Not Active",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.warningCardText
                    )
                    Text(
                        text = if (!isImeEnabled) {
                            "Enable Kiebord in Android system settings to begin typing."
                        } else {
                            "Kiebord is enabled, but not set as your active input method."
                        },
                        fontSize = 12.sp,
                        color = colors.warningCardSubtext,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = if (!isImeEnabled) onOpenSettings else onSwitchIme,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.warningBtnBg,
                                contentColor = colors.warningBtnText
                            )
                        ) {
                            Icon(
                                imageVector = if (!isImeEnabled) Icons.Outlined.Settings else Icons.Outlined.SwapHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (!isImeEnabled) "Turn On" else "Switch Back",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = { onNavigate(SettingsScreen.Onboarding) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.iconBackground,
                                contentColor = colors.textPrimary
                            )
                        ) {
                            Text(
                                text = "Setup Wizard",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.activeCardBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = colors.activeCardIcon,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kiebord is Active",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.activeCardText
                        )
                        Text(
                            text = "Ready to type in all applications",
                            fontSize = 11.sp,
                            color = colors.activeCardSubtext
                        )
                    }
                    Button(
                        onClick = { onNavigate(SettingsScreen.Onboarding) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.activeCardBg,
                            contentColor = colors.activeCardText
                        )
                    ) {
                        Text(
                            text = "Wizard",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        SettingsCategoryHeader(title = "Configuration")

        SettingsCard {
            SettingsNavigationItem(
                title = "Appearance & Themes",
                icon = Icons.Outlined.Palette,
                badge = themeBadge,
                onClick = { onNavigate(SettingsScreen.AppearanceThemes) }
            )

            SettingsNavigationItem(
                title = "Keyboard Layouts",
                icon = Icons.Outlined.Keyboard,
                badge = layoutBadge,
                onClick = { onNavigate(SettingsScreen.KeyboardLayouts) }
            )

            SettingsNavigationItem(
                title = "Preferences",
                icon = Icons.Outlined.Tune,
                onClick = { onNavigate(SettingsScreen.Preferences) }
            )

            SettingsNavigationItem(
                title = "Corrections & Suggestions",
                icon = Icons.Outlined.Spellcheck,
                showDivider = false,
                onClick = { onNavigate(SettingsScreen.CorrectionsSuggestions) }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        SettingsCategoryHeader(title = "System & Security")

        SettingsCard {
            SettingsNavigationItem(
                title = "About Kiebord",
                icon = Icons.Outlined.Info,
                badge = "v0.1.0",
                showDivider = false,
                onClick = { onNavigate(SettingsScreen.About) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        SettingsCategoryHeader(title = "Interactive Test Sandbox")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tap below to test your settings live:",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = testText,
                    onValueChange = { testText = it },
                    placeholder = {
                        Text(text = "Try typing in English or বাংলা...", color = colors.textSecondary.copy(alpha = 0.7f))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.inputBackground,
                        unfocusedContainerColor = colors.inputBackground,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.inputBorder,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
