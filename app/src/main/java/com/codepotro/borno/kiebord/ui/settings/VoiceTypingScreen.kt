package com.codepotro.borno.kiebord.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.codepotro.borno.data.prefs.KiebordPreferences
import com.codepotro.borno.feature.ime.voice.VoiceInputManager

@Composable
fun VoiceTypingScreen(
    preferences: KiebordPreferences,
    onUpdateVoiceTypingEnabled: (Boolean) -> Unit,
    onUpdateVoiceTypingProvider: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val colors = LocalSettingsColors.current
    val context = LocalContext.current

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1. Master Toggle
        SettingsGroup {
            SettingsToggleItem(
                title = "Enable Voice Typing",
                subtitle = "Show microphone voice key on keyboard toolbar for dictation",
                icon = Icons.Outlined.Mic,
                checked = preferences.voiceTypingEnabled,
                position = GroupPosition.ALONE,
                onCheckedChange = onUpdateVoiceTypingEnabled
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Voice Typing Engine Selection
        SettingsCategoryHeader(title = "Voice Typing Engine")

        SettingsGroup {
            SettingsRadioOption(
                title = "Auto (Inline with Popup Fallback)",
                subtitle = "Attempts continuous inline typing; seamlessly falls back to Google Voice dialog if system restricts background voice services",
                selected = preferences.voiceTypingProvider == KiebordPreferences.VOICE_PROVIDER_GOOGLE,
                position = GroupPosition.TOP,
                onSelect = { onUpdateVoiceTypingProvider(KiebordPreferences.VOICE_PROVIDER_GOOGLE) }
            )

            SettingsRadioOption(
                title = "Always Inline (Continuous Dictation)",
                subtitle = "Keeps dictation strictly on the keyboard toolbar. Displays diagnostic error codes if system blocks background voice services.",
                selected = preferences.voiceTypingProvider == KiebordPreferences.VOICE_PROVIDER_INLINE,
                position = GroupPosition.MIDDLE,
                onSelect = { onUpdateVoiceTypingProvider(KiebordPreferences.VOICE_PROVIDER_INLINE) }
            )

            SettingsRadioOption(
                title = "Google Voice Dialog (Popup)",
                subtitle = "Guaranteed 100% compatibility for Chinese ROMs (Vivo OriginOS, Xiaomi, Oppo). Completely immune to background blocks.",
                selected = preferences.voiceTypingProvider == KiebordPreferences.VOICE_PROVIDER_POPUP,
                position = GroupPosition.BOTTOM,
                onSelect = { onUpdateVoiceTypingProvider(KiebordPreferences.VOICE_PROVIDER_POPUP) }
            )
        }

        val isSystemVoiceServiceSet = remember(context) {
            try {
                !Settings.Secure.getString(context.contentResolver, "voice_recognition_service").isNullOrBlank()
            } catch (_: Exception) {
                true
            }
        }

        if (!isSystemVoiceServiceSet && preferences.voiceTypingProvider != KiebordPreferences.VOICE_PROVIDER_POPUP) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Set Default Voice Service for Inline",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "On Vivo OriginOS, Google is not configured as the default voice service out of the box. Tap below to select Google in 1 tap, or choose Google Voice Dialog (Popup) which works without any setup.",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                    )
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open Voice Settings", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (preferences.voiceTypingProvider == KiebordPreferences.VOICE_PROVIDER_INLINE) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Tips for Vivo OriginOS / Chinese ROMs",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent
                    )
                    Text(
                        text = "1. Install the main Google app (Google Search) from Play Store.\n2. In phone Settings > Apps > Default apps > Assist app & voice input, set Default assist app to Google.\n3. In Settings > Battery > Background power consumption, grant High background power for both Kiebord and Google.\n4. In Permissions, enable Autostart and Associated startup.",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        if (preferences.voiceTypingProvider == KiebordPreferences.VOICE_PROVIDER_GOOGLE) {
            val voicePrefs = remember(context) {
                context.getSharedPreferences("kiebord_voice_prefs", Context.MODE_PRIVATE)
            }
            var isAutoFallbackActive by remember {
                mutableStateOf(voicePrefs.getBoolean("auto_fallback_popup", false))
            }
            if (isAutoFallbackActive) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "System Restriction Detected",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Using Google Voice Popup dialog to bypass ROM background blocks.",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        TextButton(
                            onClick = {
                                VoiceInputManager.resetPopupDetection(context)
                                isAutoFallbackActive = false
                                Toast.makeText(context, "Detection reset to Auto", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Reset", color = colors.accent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Microphone Permission Status Card
        SettingsCategoryHeader(title = "Microphone Permission")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (hasMicPermission) colors.activeCardBg else colors.warningCardBg
            ),
            border = BorderStroke(
                1.dp,
                if (hasMicPermission) colors.activeCardBorder else colors.warningCardBorder
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (hasMicPermission) Icons.Outlined.CheckCircle else Icons.Outlined.MicOff,
                    contentDescription = null,
                    tint = if (hasMicPermission) colors.activeCardIcon else colors.warningCardText,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasMicPermission) "Microphone Access Granted" else "Microphone Permission Needed",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasMicPermission) colors.activeCardText else colors.warningCardText
                    )
                    Text(
                        text = if (hasMicPermission) {
                            "Ready to transcribe voice according to your current keyboard layout language."
                        } else {
                            "Microphone access is required to capture audio for speech recognition."
                        },
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = if (hasMicPermission) colors.activeCardSubtext else colors.warningCardSubtext,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (!hasMicPermission) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        shape = CircleShape,
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.warningBtnBg,
                            contentColor = colors.warningBtnText
                        )
                    ) {
                        Text(
                            text = "Grant",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
