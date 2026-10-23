package io.kiebord.app.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.BorderStyle
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.data.prefs.KiebordPreferences
import io.kiebord.theme.KeyboardColorScheme

private data class ThemeOption(
    val id: String,
    val name: String,
    val description: String,
    val scheme: KeyboardColorScheme
)

@Composable
fun AppearanceThemesScreen(
    preferences: KiebordPreferences,
    onUpdateTheme: (String) -> Unit,
    onUpdateHeightScale: (Float) -> Unit,
    onUpdateShowNumberRow: (Boolean) -> Unit,
    onUpdateShowKeyBorders: (Boolean) -> Unit,
    onUpdateShowKeyPopups: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    val themes = listOf(
        ThemeOption(
            id = KiebordPreferences.THEME_DEFAULT_DARK,
            name = "Default Dark",
            description = "Sleek charcoal with vibrant Tokyo Indigo accents",
            scheme = KeyboardColorScheme.DefaultDark
        ),
        ThemeOption(
            id = KiebordPreferences.THEME_AMOLED_BLACK,
            name = "AMOLED Pure Black",
            description = "True pitch black for maximum OLED battery savings",
            scheme = KeyboardColorScheme.AmoledBlack
        ),
        ThemeOption(
            id = KiebordPreferences.THEME_TOKYO_NIGHT,
            name = "Tokyo Night",
            description = "Deep indigo twilight inspired by neon cityscapes",
            scheme = KeyboardColorScheme.TokyoNight
        ),
        ThemeOption(
            id = KiebordPreferences.THEME_NORD,
            name = "Nord Frost",
            description = "Arctic slate blue and cool glacial hues",
            scheme = KeyboardColorScheme.Nord
        ),
        ThemeOption(
            id = KiebordPreferences.THEME_DEFAULT_LIGHT,
            name = "Default Light",
            description = "Clean, high-contrast crisp white paper aesthetic",
            scheme = KeyboardColorScheme.DefaultLight
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        SettingsCategoryHeader(title = "Color Themes")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            themes.chunked(3).forEach { rowThemes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowThemes.forEach { themeOption ->
                        val isSelected = preferences.theme == themeOption.id
                        AbstractThemeCard(
                            themeOption = themeOption,
                            isSelected = isSelected,
                            modifier = Modifier.weight(1f),
                            onSelect = { onUpdateTheme(themeOption.id) }
                        )
                    }
                    repeat(3 - rowThemes.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsCategoryHeader(title = "Keyboard Dimensions & Scaling")

        val heightScale = preferences.keyboardHeightScale
        val heightLabel = when {
            heightScale <= 0.88f -> "85% (Compact)"
            heightScale <= 0.96f -> "95% (Slim)"
            heightScale <= 1.05f -> "100% (Standard)"
            heightScale <= 1.16f -> "115% (Comfortable)"
            else -> "125% (Large)"
        }

        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsSliderItem(
                title = "Keyboard Height",
                subtitle = "Scale vertical height of keycaps and row spacing",
                icon = Icons.Outlined.AspectRatio,
                value = heightScale,
                valueRange = 0.85f..1.25f,
                steps = 7,
                valueLabel = heightLabel,
                showDivider = false,
                onValueChange = onUpdateHeightScale
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategoryHeader(title = "Keycap Styling & Rows")

        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggleItem(
                title = "Dedicated Number Row",
                subtitle = "Display persistent 5th row of digits above letter keys",
                icon = Icons.Outlined.Numbers,
                checked = preferences.showNumberRow,
                onCheckedChange = onUpdateShowNumberRow
            )

            SettingsToggleItem(
                title = "Keycap Outlines",
                subtitle = "Draw crisp separation borders around individual keys",
                icon = Icons.Outlined.BorderStyle,
                checked = preferences.showKeyBorders,
                onCheckedChange = onUpdateShowKeyBorders
            )

            SettingsToggleItem(
                title = "Popup on Keypress",
                subtitle = "Show magnified character pill when touching keys",
                icon = Icons.Outlined.Layers,
                checked = preferences.showKeyPopups,
                showDivider = false,
                onCheckedChange = onUpdateShowKeyPopups
            )
        }
    }
}

@Composable
private fun AbstractThemeCard(
    themeOption: ThemeOption,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF7AA2F7) else Color(0xFF2B2D31),
        animationSpec = tween(durationMillis = 200),
        label = "themeBorder"
    )

    Column(
        modifier = modifier.clickable(onClick = onSelect),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(themeOption.scheme.containerBackground)
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(8.dp)
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7AA2F7))
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color(0xFF0F141C),
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            // Abstract Swatches: Space key color & Enter key color on solid theme container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Space key swatch
                Box(
                    modifier = Modifier
                        .weight(3f)
                        .height(13.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(themeOption.scheme.keyBackground)
                        .border(
                            0.5.dp,
                            themeOption.scheme.keyBorder.copy(alpha = 0.5f),
                            RoundedCornerShape(3.dp)
                        )
                )

                // Enter key swatch
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .height(13.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(themeOption.scheme.actionKeyBackground)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = themeOption.name,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF7AA2F7) else Color(0xFFE0E2E8),
            maxLines = 1
        )
    }
}
