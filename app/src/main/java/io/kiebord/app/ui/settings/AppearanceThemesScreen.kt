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
import androidx.compose.ui.unit.TextUnit
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

    // Modal Bottom Sheet showing Demo Keyboard with QWERTY layout and Key borders toggle
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
                        .border(
                            0.5.dp,
                            themeOption.scheme.keyBorder.copy(alpha = 0.5f),
                            RoundedCornerShape(3.dp)
                        )
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
    showKeyBorders: Boolean,
    onToggleKeyBorders: (Boolean) -> Unit,
    onApplyTheme: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
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
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
        )

        // Demo Keyboard with QWERTY layout
        DemoQwertyKeyboard(
            scheme = themeOption.scheme,
            showKeyBorders = showKeyBorders
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Key Border Toggle (moved below demo keyboard preview)
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
                    text = "Toggle flat keycaps (like Borno Keyboard) and default key borders",
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

        Spacer(modifier = Modifier.height(18.dp))

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

@Composable
private fun DemoQwertyKeyboard(
    scheme: KeyboardColorScheme,
    showKeyBorders: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.containerBackground)
            .border(1.dp, Color(0xFF2E3138), RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        val row3 = listOf("z", "x", "c", "v", "b", "n", "m")
        val pillHeight = if (showKeyBorders) 34.dp else 26.dp
        val pillShape = if (showKeyBorders) RoundedCornerShape(6.dp) else RoundedCornerShape(13.dp)

        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            row1.forEach { key ->
                DemoKeycap(
                    label = key,
                    modifier = Modifier.weight(1f),
                    scheme = scheme,
                    showKeyBorders = showKeyBorders
                )
            }
        }

        // Row 2 (centered)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Spacer(modifier = Modifier.weight(0.5f))
            row2.forEach { key ->
                DemoKeycap(
                    label = key,
                    modifier = Modifier.weight(1f),
                    scheme = scheme,
                    showKeyBorders = showKeyBorders
                )
            }
            Spacer(modifier = Modifier.weight(0.5f))
        }

        // Row 3: Shift, keys, Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DemoKeycap(
                label = "⇧",
                modifier = Modifier.weight(1.3f),
                scheme = scheme,
                showKeyBorders = showKeyBorders,
                isActionKey = true
            )
            row3.forEach { key ->
                DemoKeycap(
                    label = key,
                    modifier = Modifier.weight(1f),
                    scheme = scheme,
                    showKeyBorders = showKeyBorders
                )
            }
            DemoKeycap(
                label = "⌫",
                modifier = Modifier.weight(1.3f),
                scheme = scheme,
                showKeyBorders = showKeyBorders,
                isActionKey = true
            )
        }

        // Row 4: ?123, comma, spacebar, dot, enter
        // In flat theme (!showKeyBorders): spacebar, sym key and enter have distinct color and reduced height
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ?123 (Sym key: distinct color and reduced height in flat mode)
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .height(pillHeight)
                    .clip(pillShape)
                    .background(scheme.actionKeyBackground)
                    .then(
                        if (showKeyBorders) Modifier.border(1.dp, scheme.keyBorder, pillShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "?123",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = scheme.actionKeyForeground
                )
            }

            // Comma (flat in flat mode)
            DemoKeycap(
                label = ",",
                modifier = Modifier.weight(0.9f),
                scheme = scheme,
                showKeyBorders = showKeyBorders
            )

            // Spacebar (distinct color and reduced height in flat mode)
            Box(
                modifier = Modifier
                    .weight(4.0f)
                    .height(pillHeight)
                    .clip(pillShape)
                    .background(scheme.keyBackground)
                    .then(
                        if (showKeyBorders) Modifier.border(1.dp, scheme.keyBorder, pillShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "English",
                    fontSize = 11.sp,
                    color = scheme.keyForeground.copy(alpha = 0.7f)
                )
            }

            // Dot (flat in flat mode)
            DemoKeycap(
                label = ".",
                modifier = Modifier.weight(0.9f),
                scheme = scheme,
                showKeyBorders = showKeyBorders
            )

            // Enter key (distinct color and reduced height in flat mode)
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .height(pillHeight)
                    .clip(pillShape)
                    .background(scheme.accentColor)
                    .then(
                        if (showKeyBorders) Modifier.border(1.dp, scheme.keyBorder, pillShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⏎",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.actionKeyForeground
                )
            }
        }
    }
}

@Composable
private fun DemoKeycap(
    label: String,
    modifier: Modifier = Modifier,
    scheme: KeyboardColorScheme,
    showKeyBorders: Boolean,
    isActionKey: Boolean = false,
    fontSize: TextUnit = 13.sp
) {
    // When showKeyBorders is false (flat theme): key color is same as containerBackground
    val bg = when {
        showKeyBorders -> if (isActionKey) scheme.actionKeyBackground else scheme.keyBackground
        else -> scheme.containerBackground
    }
    val borderMod = if (showKeyBorders) {
        Modifier.border(1.dp, scheme.keyBorder, RoundedCornerShape(6.dp))
    } else {
        Modifier
    }
    val textColor = if (isActionKey) scheme.actionKeyForeground else scheme.keyForeground

    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .then(borderMod),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
