package io.kiebord.app.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.app.ui.settings.LocalSettingsColors
import io.kiebord.common.model.ShiftState
import io.kiebord.data.layout.PredefinedLayouts
import io.kiebord.data.prefs.KiebordPreferences
import io.kiebord.feature.ime.ui.KeyboardRootView
import io.kiebord.theme.KeyboardColorScheme
import io.kiebord.theme.KiebordTheme

@Composable
fun OnboardingPreferencesStep(
    primaryLayoutId: String,
    theme: String,
    initialShowNumberRow: Boolean,
    initialShowSecondaryKeys: Boolean,
    initialShowKeyBorders: Boolean,
    onFinish: (showNumberRow: Boolean, showSecondaryKeys: Boolean, showKeyBorders: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSettingsColors.current
    val scrollState = rememberScrollState()

    var showNumberRow by remember { mutableStateOf(initialShowNumberRow) }
    var showSecondaryKeys by remember { mutableStateOf(initialShowSecondaryKeys) }
    var showKeyBorders by remember { mutableStateOf(initialShowKeyBorders) }

    val layoutDef = remember(primaryLayoutId) {
        PredefinedLayouts.fromId(primaryLayoutId)
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val keyboardColorScheme = remember(context, theme) {
        io.kiebord.theme.ThemeRepository.getInstance(context).getColorScheme(theme)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
            .padding(bottom = 36.dp)
    ) {
        // Step progress pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(colors.accent.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Step 4 of 4",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.accent
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Personalize Your Typing",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Customize borders, quick number access, and secondary key hints. The keyboard below updates live with your choices.",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Live Real Keyboard View Preview
        Text(
            text = "REAL KEYBOARD PREVIEW",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colors.accent,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                .background(keyboardColorScheme.containerBackground)
        ) {
            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
            val baseWidth = if (screenWidth > 500.dp) 390.dp else screenWidth
            val widthRatio = if (baseWidth > 0.dp) (maxWidth / baseWidth).coerceIn(0.5f, 1.0f) else 0.88f
            val demoBase = if (showNumberRow) 54 else 61
            val demoRowHeight = (demoBase * widthRatio).dp

            KiebordTheme(colorScheme = keyboardColorScheme) {
                KeyboardRootView(
                    layout = layoutDef,
                    shiftState = ShiftState.UNSHIFTED,
                    onKeyTap = {},
                    onKeyLongPress = {},
                    showKeyBorders = showKeyBorders,
                    defaultShowNumberRow = showNumberRow,
                    defaultShowSecondaryKeys = showSecondaryKeys,
                    initialRowHeight = demoRowHeight,
                    applySystemInsets = false,
                    isInteractive = false,
                    navBarBottomInset = 0.dp
                )
            }

            // Transparent overlay interceptor so preview is strictly non-touchable/non-flickering
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Option Toggles
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Toggle 1: Key Borders
            PreferenceToggleCard(
                title = "Key Borders",
                checked = showKeyBorders,
                onCheckedChange = { showKeyBorders = it }
            )

            // Toggle 2: Number Row
            PreferenceToggleCard(
                title = "Number Row",
                checked = showNumberRow,
                onCheckedChange = { showNumberRow = it }
            )

            // Toggle 3: Show Secondary Keys
            PreferenceToggleCard(
                title = "Show Secondary Keys",
                checked = showSecondaryKeys,
                onCheckedChange = { showSecondaryKeys = it }
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Finish Setup Button (full rounded radius)
        Button(
            onClick = { onFinish(showNumberRow, showSecondaryKeys, showKeyBorders) },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Finish Setup",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PreferenceToggleCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalSettingsColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) },
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = colors.accent,
                    uncheckedThumbColor = colors.textSecondary,
                    uncheckedTrackColor = colors.iconBackground
                )
            )
        }
    }
}
