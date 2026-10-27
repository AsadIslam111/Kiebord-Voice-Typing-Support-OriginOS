package io.kiebord.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
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
import io.kiebord.data.prefs.KiebordPreferences

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

        // 2. Provider Selection
        SettingsCategoryHeader(title = "Speech Recognition Provider")

        SettingsGroup {
            SettingsRadioOption(
                title = "Google Speech Recognizer",
                subtitle = "Default speech recognition engine with live continuous dictation",
                selected = preferences.voiceTypingProvider == KiebordPreferences.VOICE_PROVIDER_GOOGLE,
                position = GroupPosition.TOP,
                onSelect = { onUpdateVoiceTypingProvider(KiebordPreferences.VOICE_PROVIDER_GOOGLE) }
            )

            SettingsRadioOption(
                title = "Offline models (Coming Soon)",
                subtitle = "",
                selected = false,
                position = GroupPosition.BOTTOM,
                onSelect = {
                    // Coming soon notice
                }
            )
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
