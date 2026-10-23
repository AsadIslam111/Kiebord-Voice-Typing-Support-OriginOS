package io.kiebord.app.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
        else -> "QWERTY"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // App Header
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2B2D31)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Keyboard,
                    contentDescription = null,
                    tint = Color(0xFF7AA2F7),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "Kiebord Settings",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE0E2E8)
                )
                Text(
                    text = "Free & Open Source",
                    fontSize = 12.sp,
                    color = Color(0xFF9499A8)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Activation / Status Card
        if (!isImeEnabled || !isImeSelected) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B271F))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (!isImeEnabled) "Keyboard Not Enabled" else "Keyboard Not Selected",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFB86C)
                    )
                    Text(
                        text = if (!isImeEnabled) {
                            "Enable Kiebord in Android system settings to begin typing."
                        } else {
                            "Switch your active input method to Kiebord."
                        },
                        fontSize = 12.sp,
                        color = Color(0xFFD4C8B8),
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    Button(
                        onClick = if (!isImeEnabled) onOpenSettings else onSwitchIme,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB86C),
                            contentColor = Color(0xFF1E1F22)
                        )
                    ) {
                        Icon(
                            imageVector = if (!isImeEnabled) Icons.Outlined.Settings else Icons.Outlined.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (!isImeEnabled) "Enable in Settings" else "Switch Input Method",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2E20))
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
                        tint = Color(0xFF81C784),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Kiebord is Active",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFA5D6A7)
                        )
                        Text(
                            text = "Ready to type in all applications",
                            fontSize = 11.sp,
                            color = Color(0xFF81C784)
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
                subtitle = "Themes, keyboard height, key borders and styling",
                icon = Icons.Outlined.Palette,
                badge = themeBadge,
                onClick = { onNavigate(SettingsScreen.AppearanceThemes) }
            )

            SettingsNavigationItem(
                title = "Keyboard Layouts",
                subtitle = "English (QWERTY), Borno Phonetic and Probhat",
                icon = Icons.Outlined.Keyboard,
                badge = layoutBadge,
                onClick = { onNavigate(SettingsScreen.KeyboardLayouts) }
            )

            SettingsNavigationItem(
                title = "Preferences",
                subtitle = "Haptic feedback, vibration, sounds and dwell delays",
                icon = Icons.Outlined.Tune,
                onClick = { onNavigate(SettingsScreen.Preferences) }
            )

            SettingsNavigationItem(
                title = "Corrections & Suggestions",
                subtitle = "Glide typing, predictive suggestions and auto-capitalization",
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
                subtitle = "100% offline privacy, Rust core architecture and version info",
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F22))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tap below to test your settings live:",
                    fontSize = 12.sp,
                    color = Color(0xFF9499A8)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = testText,
                    onValueChange = { testText = it },
                    placeholder = {
                        Text(text = "Try typing in English or বাংলা...", color = Color(0xFF6B7280))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2B2D31),
                        unfocusedContainerColor = Color(0xFF25272B),
                        focusedBorderColor = Color(0xFF7AA2F7),
                        unfocusedBorderColor = Color(0xFF383A40),
                        focusedTextColor = Color(0xFFE0E2E8),
                        unfocusedTextColor = Color(0xFFE0E2E8)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
