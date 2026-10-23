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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import io.kiebord.common.model.ShiftState
import io.kiebord.data.layout.PredefinedLayouts
import io.kiebord.data.prefs.KiebordPreferences
import io.kiebord.feature.ime.ui.KeyboardRootView
import io.kiebord.theme.KeyboardColorScheme
import io.kiebord.theme.KiebordTheme

private data class ThemeOption(
    val id: String,
    val name: String,
    val description: String,
    val scheme: KeyboardColorScheme
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceThemesScreen(
    preferences: KiebordPreferences,
    onUpdateTheme: (String) -> Unit,
    onUpdateHeightScale: (Float) -> Unit,
    onUpdateShowKeyBorders: (Boolean) -> Unit,
    onBack: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var previewThemeOption by remember { mutableStateOf<ThemeOption?>(null) }

    val themes = listOf(
        ThemeOption(
            id = KiebordPreferences.THEME_DEFAULT_DARK,
            name = "Default Dark",
            description = "Sleek charcoal with vibrant Tokyo Indigo accents",
            scheme = KeyboardColorScheme.DefaultDark
        ),
        ThemeOption(
            id = KiebordPreferences.THEME_AMOLED_BLACK,
            name = "AMOLED Black",
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
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            themes.chunked(3).forEach { rowThemes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowThemes.forEach { themeOption ->
                        val isSelected = preferences.theme == themeOption.id
                        AbstractThemeCard(
                            themeOption = themeOption,
                            isSelected = isSelected,
                            modifier = Modifier.weight(1f),
                            onSelect = { previewThemeOption = themeOption }
                        )
                    }
                    repeat(3 - rowThemes.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
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
    }

    // Modal Bottom Sheet showing real actual KeyboardRootView in theme colors and Key borders toggle
    if (previewThemeOption != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { previewThemeOption = null },
            sheetState = sheetState,
            containerColor = Color(0xFF1E1F22),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            ThemePreviewBottomSheetContent(
                themeOption = previewThemeOption!!,
                preferences = preferences,
                showKeyBorders = preferences.showKeyBorders,
                onToggleKeyBorders = onUpdateShowKeyBorders,
                onApplyTheme = {
                    onUpdateTheme(previewThemeOption!!.id)
                    previewThemeOption = null
                }
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
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(themeOption.scheme.containerBackground)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
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

            // Abstract Swatches: Space key & compact Enter key on solid theme container (reduced height)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Space key swatch (reduced height)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(themeOption.scheme.keyBackground)
                )

                // Compact Enter key swatch (reduced height)
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(themeOption.scheme.actionKeyBackground)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = themeOption.name,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF7AA2F7) else Color(0xFFE0E2E8),
            maxLines = 1
        )
    }
}

@Composable
private fun ThemePreviewBottomSheetContent(
    themeOption: ThemeOption,
    preferences: KiebordPreferences,
    showKeyBorders: Boolean,
    onToggleKeyBorders: (Boolean) -> Unit,
    onApplyTheme: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = themeOption.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE0E2E8)
            )
            Text(
                text = themeOption.description,
                fontSize = 12.sp,
                color = Color(0xFF9499A8),
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )
        }

        // Live Actual Keyboard View preview in selected theme color scheme (exact match to real keyboard dimensions, padding & UI)
        KiebordTheme(colorScheme = themeOption.scheme) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeOption.scheme.containerBackground)
            ) {
                KeyboardRootView(
                    layout = PredefinedLayouts.EnglishQwerty,
                    shiftState = ShiftState.UNSHIFTED,
                    onKeyTap = {},
                    onKeyLongPress = {},
                    showKeyBorders = showKeyBorders,
                    defaultShowNumberRow = preferences.showNumberRow,
                    initialRowHeight = (60 * preferences.keyboardHeightScale).dp,
                    applySystemInsets = false,
                    navBarBottomInset = 0.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Key Border Toggle & Action buttons with standard margins
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Key Border Toggle (located directly below real keyboard preview)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF25272B))
                    .clickable { onToggleKeyBorders(!showKeyBorders) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Key borders",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE0E2E8)
                    )
                    Text(
                        text = if (showKeyBorders) "Keyborder mode enabled" else "Flat mode enabled",
                        fontSize = 12.sp,
                        color = Color(0xFF9499A8)
                    )
                }
                Switch(
                    checked = showKeyBorders,
                    onCheckedChange = onToggleKeyBorders,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF7AA2F7),
                        uncheckedThumbColor = Color(0xFF9499A8),
                        uncheckedTrackColor = Color(0xFF383A40)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onApplyTheme,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7AA2F7)
                )
            ) {
                Text(
                    text = "Apply Theme",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F141C)
                )
            }
        }
    }
}
