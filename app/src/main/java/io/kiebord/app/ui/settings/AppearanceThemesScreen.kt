package io.kiebord.app.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.common.model.ShiftState
import io.kiebord.data.layout.PredefinedLayouts
import io.kiebord.data.prefs.KiebordPreferences
import io.kiebord.feature.ime.ui.KeyboardRootView
import io.kiebord.feature.ime.ui.ScaledKeyboardPreview
import io.kiebord.theme.KiebordTheme
import io.kiebord.theme.ThemeCategory
import io.kiebord.theme.ThemeColorUtils
import io.kiebord.theme.ThemeOptionItem
import io.kiebord.theme.ThemeRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceThemesScreen(
    preferences: KiebordPreferences,
    onUpdateTheme: (String) -> Unit,
    onUpdateHeightScale: (Float) -> Unit = {},
    onUpdateShowKeyBorders: (Boolean) -> Unit,
    onUpdateShowKeyShadows: (Boolean) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val themeRepo = remember(context) { ThemeRepository.getInstance(context) }
    var reloadTrigger by remember { mutableStateOf(0) }
    val allThemes = remember(reloadTrigger) { themeRepo.getAvailableThemes() }

    val scrollState = rememberScrollState()
    var previewThemeOption by remember { mutableStateOf<ThemeOptionItem?>(null) }
    val colors = LocalSettingsColors.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val jsonContent = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                }
                if (jsonContent != null) {
                    val result = themeRepo.importTheme(jsonContent)
                    result.onSuccess { importedList ->
                        reloadTrigger++
                        if (importedList.isNotEmpty()) {
                            onUpdateTheme(importedList.first().id)
                            Toast.makeText(context, "Imported ${importedList.size} theme(s)", Toast.LENGTH_SHORT).show()
                        }
                    }.onFailure { err ->
                        Toast.makeText(context, "Import failed: ${err.message ?: "Invalid JSON"}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val categories = listOf(
        ThemeCategory.ORIGINALS,
        ThemeCategory.BORNO_GRADIENT,
        ThemeCategory.BORNO_COLORS,
        ThemeCategory.CUSTOM
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Create",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            modifier = Modifier.padding(start = 18.dp, top = 22.dp, bottom = 14.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BlankThemePlusCard(
                modifier = Modifier.weight(1f),
                onClick = {
                    Toast.makeText(context, "Not yet implemented", Toast.LENGTH_SHORT).show()
                }
            )
            repeat(2) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        for (category in categories) {
            val categoryThemes = allThemes.filter { it.category == category }
            if (categoryThemes.isNotEmpty()) {
                Text(
                    text = category.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(start = 18.dp, top = 22.dp, bottom = 14.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    categoryThemes.chunked(3).forEach { rowThemes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            rowThemes.forEach { themeOption ->
                                val isSelected = preferences.theme == themeOption.id
                                AbstractThemeCard(
                                    themeOption = themeOption,
                                    isSelected = isSelected,
                                    showKeyBorders = preferences.showKeyBorders,
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
            }
        }
    }

    if (previewThemeOption != null) {
        val currentPreview = previewThemeOption
        if (currentPreview != null) {
            var previewShowKeyBorders by remember(currentPreview.id, preferences.showKeyBorders) {
                mutableStateOf(preferences.showKeyBorders)
            }
            var previewShowKeyShadows by remember(currentPreview.id, preferences.showKeyShadows) {
                mutableStateOf(preferences.showKeyShadows)
            }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { previewThemeOption = null },
                sheetState = sheetState,
                containerColor = colors.cardBackground,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                ThemePreviewBottomSheetContent(
                    themeOption = currentPreview,
                    preferences = preferences,
                    showKeyBorders = previewShowKeyBorders,
                    showKeyShadows = previewShowKeyShadows,
                    onToggleKeyBorders = { previewShowKeyBorders = it },
                    onToggleKeyShadows = { previewShowKeyShadows = it },
                    onApplyTheme = {
                        onUpdateTheme(currentPreview.id)
                        onUpdateShowKeyBorders(previewShowKeyBorders)
                        onUpdateShowKeyShadows(previewShowKeyShadows)
                        previewThemeOption = null
                    },
                    onDeleteTheme = if (currentPreview.isCustom) {
                        {
                            val targetId = currentPreview.id
                            val targetName = currentPreview.name
                            themeRepo.deleteCustomTheme(targetId)
                            if (preferences.theme == targetId) {
                                onUpdateTheme(KiebordPreferences.THEME_DEFAULT_DARK)
                            }
                            reloadTrigger++
                            previewThemeOption = null
                            Toast.makeText(context, "Deleted $targetName", Toast.LENGTH_SHORT).show()
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun BlankThemePlusCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalSettingsColors.current
    val cardShape = RoundedCornerShape(20.dp)
    val borderColor = if (colors.isDark) Color(0x18FFFFFF) else Color(0x0E000000)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.3f)
            .shadow(
                elevation = 2.dp,
                shape = cardShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.25f)
            )
            .clip(cardShape)
            .background(colors.cardBackground)
            .border(width = 1.dp, color = borderColor, shape = cardShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = "Create Theme",
            tint = colors.textSecondary,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun AbstractThemeCard(
    themeOption: ThemeOptionItem,
    isSelected: Boolean,
    showKeyBorders: Boolean = true,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    val colors = LocalSettingsColors.current
    val scheme = themeOption.scheme

    val gradientBrush = remember(scheme) {
        scheme.themeSpec?.background?.gradient?.toBrush(androidx.compose.ui.geometry.Size(200f, 150f))
    }

    val cardShape = RoundedCornerShape(20.dp)
    val cardBorderColor by animateColorAsState(
        targetValue = if (isSelected) colors.accent else if (colors.isDark) Color(0x18FFFFFF) else Color(0x0E000000),
        animationSpec = tween(durationMillis = 200),
        label = "themeBorder"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.3f)
            .shadow(
                elevation = if (isSelected) 4.dp else 2.dp,
                shape = cardShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.25f)
            )
            .clip(cardShape)
            .then(
                if (gradientBrush != null) {
                    Modifier.background(gradientBrush)
                } else {
                    Modifier.background(scheme.containerBackground)
                }
            )
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = cardBorderColor,
                shape = cardShape
            )
            .clickable(onClick = onSelect)
    ) {
        val cardWidth = maxWidth
        val cardHeight = maxHeight

        val keyHeight = cardHeight * 0.15f
        val bottomMargin = cardHeight * 0.14f
        val sideMargin = cardWidth * 0.14f
        val keyGap = 6.dp

        val keyShape = RoundedCornerShape(4.dp)
        val keyBorderColor = scheme.keyBorder
        val hasStroke = showKeyBorders && keyBorderColor != Color.Transparent && keyBorderColor.alpha > 0.05f

        val spaceHex = scheme.themeSpec?.spaceKey?.background ?: scheme.themeSpec?.characterKey?.background
        val spaceColor = if (spaceHex != null) ThemeColorUtils.parseHexColor(spaceHex, scheme.keyBackground) else scheme.keyBackground

        val enterHex = scheme.themeSpec?.actionKey?.background
        val enterColor = if (enterHex != null) ThemeColorUtils.parseHexColor(enterHex, scheme.accentColor) else scheme.accentColor

        if (themeOption.isCustom && !isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.accent.copy(alpha = 0.25f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "CUSTOM",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = sideMargin, end = sideMargin, bottom = bottomMargin)
                .height(keyHeight),
            horizontalArrangement = Arrangement.spacedBy(keyGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .then(
                        if (hasStroke) {
                            Modifier.border(0.75.dp, keyBorderColor, keyShape)
                        } else {
                            Modifier
                        }
                    )
                    .clip(keyShape)
                    .background(spaceColor)
            )

            Box(
                modifier = Modifier
                    .weight(0.14f)
                    .fillMaxHeight()
                    .then(
                        if (hasStroke) {
                            Modifier.border(0.75.dp, keyBorderColor, keyShape)
                        } else {
                            Modifier
                        }
                    )
                    .clip(keyShape)
                    .background(enterColor)
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .border(1.5.dp, Color.White, CircleShape)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ThemePreviewBottomSheetContent(
    themeOption: ThemeOptionItem,
    preferences: KiebordPreferences,
    showKeyBorders: Boolean,
    showKeyShadows: Boolean,
    onToggleKeyBorders: (Boolean) -> Unit,
    onToggleKeyShadows: (Boolean) -> Unit,
    onApplyTheme: () -> Unit,
    onDeleteTheme: (() -> Unit)? = null
) {
    val colors = LocalSettingsColors.current

    val themeHasShadow = remember(themeOption.scheme) {
        val s = themeOption.scheme
        s.keyShadow.alpha > 0.01f ||
            (s.themeSpec?.characterKey?.shadow != null &&
             (s.themeSpec?.characterKey?.shadow?.elevationDp ?: 0f) > 0f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = themeOption.name,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        KiebordTheme(colorScheme = themeOption.scheme) {
            BoxWithConstraints(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                    .background(themeOption.scheme.containerBackground)
            ) {
                val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                val baseWidth = if (screenWidth > 500.dp) 390.dp else screenWidth
                val fullRowHeight = if (preferences.showNumberRow) 54.dp else 61.dp
                val targetRowHeight = fullRowHeight * preferences.keyboardHeightScale

                ScaledKeyboardPreview(
                    baseWidth = baseWidth,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    KeyboardRootView(
                        layout = PredefinedLayouts.EnglishQwerty,
                        shiftState = ShiftState.UNSHIFTED,
                        onKeyTap = {},
                        onKeyLongPress = {},
                        showKeyBorders = showKeyBorders,
                        showKeyShadows = showKeyShadows,
                        defaultShowNumberRow = preferences.showNumberRow,
                        defaultShowSecondaryKeys = false,
                        initialRowHeight = targetRowHeight,
                        heightScalePortrait = preferences.keyboardHeightScale,
                        applySystemInsets = false,
                        isInteractive = false,
                        navBarBottomInset = 0.dp
                    )
                }

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
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            SettingsGroup {
                SettingsToggleItem(
                    title = "Key Borders",
                    checked = showKeyBorders,
                    position = if (themeHasShadow) GroupPosition.TOP else GroupPosition.ALONE,
                    onCheckedChange = onToggleKeyBorders
                )
                if (themeHasShadow) {
                    SettingsToggleItem(
                        title = "Key Shadows",
                        checked = showKeyShadows,
                        position = GroupPosition.BOTTOM,
                        onCheckedChange = onToggleKeyShadows
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onApplyTheme,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent
                )
            ) {
                Text(
                    text = "Apply Theme",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (colors.isDark) Color(0xFF0F141C) else Color.White
                )
            }

            if (onDeleteTheme != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onDeleteTheme,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFE53935))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete Custom Theme",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE53935)
                    )
                }
            }
        }
    }
}
