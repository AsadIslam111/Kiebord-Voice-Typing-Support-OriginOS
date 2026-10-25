package io.kiebord.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.app.R

private fun openUrl(context: Context, url: String) {
    if (url == "#" || url.isBlank()) {
        Toast.makeText(context, "Repository link coming soon", Toast.LENGTH_SHORT).show()
        return
    }
    val targetUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
        "https://$url"
    } else {
        url
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun AboutCircularButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    val colors = LocalSettingsColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(88.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(containerColor)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AboutScreen(
    onResetDefaults: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val colors = LocalSettingsColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Hero Brand Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "Kiebord Logo",
                modifier = Modifier.size(84.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Kiebord",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.iconBackground)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Version 0.1.0",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Kiebord is a FREE, Open Source IME for Android",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        SettingsCategoryHeader(title = "About & Licensing")

        SettingsGroup {
            SettingsNavigationItem(
                title = "Developer",
                subtitle = "Jayed Ahsan Saad",
                icon = Icons.Outlined.Person,
                position = GroupPosition.TOP,
                onClick = { openUrl(context, "https://jayed.me") }
            )

            SettingsNavigationItem(
                title = "License",
                subtitle = "The app and its codes are licensed under GPL 3.0",
                icon = Icons.Outlined.Policy,
                position = GroupPosition.MIDDLE,
                onClick = {}
            )

            SettingsNavigationItem(
                title = "Git Repository",
                subtitle = "GitHub • Repository link coming soon (#)",
                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                position = GroupPosition.BOTTOM,
                onClick = { openUrl(context, "#") }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Circular Action Buttons with text labels below (moved below About & Licensing, Support at last)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            // Git Repo button
            AboutCircularButton(
                icon = Icons.Outlined.Code,
                label = "Git Repo",
                containerColor = colors.iconBackground,
                contentColor = colors.textPrimary,
                onClick = { openUrl(context, "#") }
            )

            // Report Issue button
            AboutCircularButton(
                icon = Icons.Outlined.BugReport,
                label = "Report Issue",
                containerColor = colors.iconBackground,
                contentColor = colors.textPrimary,
                onClick = { openUrl(context, "#") }
            )

            // Support / Donate button with coffee mug (at last)
            AboutCircularButton(
                icon = Icons.Outlined.Coffee,
                label = "Donate",
                containerColor = colors.accent,
                contentColor = Color.White,
                onClick = { openUrl(context, "https://jayed.me/support-kiebord") }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
