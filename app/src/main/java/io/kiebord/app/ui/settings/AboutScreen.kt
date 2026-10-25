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
import androidx.compose.foundation.Image
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.app.R

@Composable
fun AboutScreen(
    onResetDefaults: () -> Unit,
    onBack: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var showResetDialog by remember { mutableStateOf(false) }
    val colors = LocalSettingsColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Hero Brand Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher),
                contentDescription = "Kiebord Logo",
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(18.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Kiebord",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.iconBackground)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Version 0.1.0 (Build 1)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent
                    )
                }
            }

            Text(
                text = "FREE & Open Source",
                fontSize = 13.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SettingsCategoryHeader(title = "Privacy & Core Principles")

        SettingsGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsNavigationItem(
                title = "100% Offline & Private",
                subtitle = "Zero internet permissions. Your keystrokes and data never leave your device.",
                icon = Icons.Outlined.Lock,
                position = GroupPosition.TOP,
                onClick = {}
            )

            SettingsNavigationItem(
                title = "Isolated Local Storage",
                subtitle = "Learned dictionary and user data are securely kept in private app sandbox.",
                icon = Icons.Outlined.Shield,
                position = GroupPosition.MIDDLE,
                onClick = {}
            )

            SettingsNavigationItem(
                title = "Open Source Engine",
                subtitle = "High-performance native transliteration core and bigram prediction models.",
                icon = Icons.Outlined.Code,
                position = GroupPosition.BOTTOM,
                onClick = {}
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategoryHeader(title = "Application Information")

        SettingsGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsNavigationItem(
                title = "Version & Build",
                subtitle = "0.1.0 (Build 1) • Production Release",
                icon = Icons.Outlined.Info,
                position = GroupPosition.TOP,
                onClick = {}
            )

            SettingsNavigationItem(
                title = "License",
                subtitle = "Free & Open Source Software (GPL-3.0 / Apache-2.0)",
                icon = Icons.Outlined.CheckCircle,
                position = GroupPosition.BOTTOM,
                onClick = {}
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategoryHeader(title = "Maintenance & Reset")

        SettingsGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsNavigationItem(
                title = "Reset All Settings to Defaults",
                subtitle = "Restore all layout, theming, haptic, and suggestion options to factory state",
                icon = Icons.Outlined.Refresh,
                iconTint = Color(0xFFF7768E),
                position = GroupPosition.ALONE,
                onClick = { showResetDialog = true }
            )
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(text = "Reset All Settings?", color = colors.textPrimary)
            },
            text = {
                Text(
                    text = "This will restore all theming, layout, haptic, and suggestion preferences back to factory defaults.",
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDefaults()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF7768E))
                ) {
                    Text(text = "Reset", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) {
                    Text(text = "Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
