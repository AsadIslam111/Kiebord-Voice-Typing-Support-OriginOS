package io.kiebord.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.common.model.KeyboardLayoutDef
import io.kiebord.common.model.ShiftState
import io.kiebord.data.layout.PredefinedLayouts
import io.kiebord.data.prefs.KiebordPreferences
import io.kiebord.feature.ime.ui.KeyboardRootView
import io.kiebord.theme.KeyboardColorScheme
import io.kiebord.theme.KiebordTheme

private data class LayoutCardData(
    val id: String,
    val name: String,
    val badge: String,
    val description: String,
    val layoutDef: KeyboardLayoutDef
)

@Composable
fun KeyboardLayoutsScreen(
    preferences: KiebordPreferences,
    onUpdatePrimaryLayout: (String) -> Unit,
    onToggleLayoutEnabled: (String, Boolean) -> Unit,
    onBack: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val colors = LocalSettingsColors.current

    val isAnyBanglaEnabled = preferences.enabledLayoutIds.any { it.startsWith("bn") }
    var isBanglaExpanded by remember {
        mutableStateOf(isAnyBanglaEnabled)
    }

    val englishCard = LayoutCardData(
        id = KiebordPreferences.LAYOUT_EN_QWERTY,
        name = "English (QWERTY)",
        badge = "EN",
        description = "Standard QWERTY layout",
        layoutDef = PredefinedLayouts.EnglishQwerty
    )

    val banglaCards = listOf(
        LayoutCardData(
            id = KiebordPreferences.LAYOUT_BN_BORNO,
            name = "Borno Phonetic",
            badge = "বর্ণ",
            description = "ami → আমি",
            layoutDef = PredefinedLayouts.BengaliBorno
        ),
        LayoutCardData(
            id = KiebordPreferences.LAYOUT_BN_PROBHAT,
            name = "Probhat",
            badge = "প্রভাত",
            description = "",
            layoutDef = PredefinedLayouts.BengaliProbhat
        ),
        LayoutCardData(
            id = KiebordPreferences.LAYOUT_BN_NATIONAL,
            name = "National (Jatiyo)",
            badge = "জাতীয়",
            description = "National BSTI layout",
            layoutDef = PredefinedLayouts.BengaliNational
        )
    )

    val isEnglishEnabled = preferences.enabledLayoutIds.contains(englishCard.id)
    val isEnglishLastRemaining = preferences.enabledLayoutIds.size == 1 && isEnglishEnabled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        SettingsCategoryHeader(title = "Available Keyboard Layouts")

        // 1. English (QWERTY) Card Container with Vertical Preview
        SettingsItemCard(
            position = GroupPosition.ALONE,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.iconBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = englishCard.badge,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = englishCard.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = englishCard.description,
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }

                    val badgeBg = if (isEnglishEnabled) {
                        colors.accent.copy(alpha = 0.15f)
                    } else colors.inputBackground
                    val badgeText = if (isEnglishEnabled) colors.accent else colors.textSecondary

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isEnglishEnabled) "1 Active" else "Disabled",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = badgeText
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 18.dp, top = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    KeyboardLayoutPreviewItem(
                        cardData = englishCard,
                        preferences = preferences,
                        isEnabled = isEnglishEnabled,
                        onToggle = {
                            if (!isEnglishLastRemaining || !isEnglishEnabled) {
                                onToggleLayoutEnabled(englishCard.id, !isEnglishEnabled)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Bangla Language Group Card with Horizontal Carousel
        SettingsItemCard(
            position = GroupPosition.ALONE,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            val activeBanglaCount = banglaCards.count { preferences.enabledLayoutIds.contains(it.id) }

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isBanglaExpanded = !isBanglaExpanded
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.iconBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "বাং",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bangla Language",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (isBanglaExpanded) "Borno Phonetic • Probhat • National" else "$activeBanglaCount active layout(s) • tap to expand",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }

                    val badgeBg = if (activeBanglaCount > 0) {
                        colors.accent.copy(alpha = 0.15f)
                    } else colors.inputBackground
                    val badgeText = if (activeBanglaCount > 0) colors.accent else colors.textSecondary

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isBanglaExpanded) "Expanded" else "$activeBanglaCount Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = badgeText
                        )
                    }
                }

                // Expanded Section: Horizontal Carousel of Vertical Keyboard Preview Cards
                AnimatedVisibility(
                    visible = isBanglaExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp, top = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            banglaCards.forEach { cardData ->
                                val isEnabled = preferences.enabledLayoutIds.contains(cardData.id)
                                val isLastRemaining = preferences.enabledLayoutIds.size == 1 && isEnabled

                                KeyboardLayoutPreviewItem(
                                    cardData = cardData,
                                    preferences = preferences,
                                    isEnabled = isEnabled,
                                    onToggle = {
                                        if (!isLastRemaining || !isEnabled) {
                                            onToggleLayoutEnabled(cardData.id, !isEnabled)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardLayoutPreviewItem(
    cardData: LayoutCardData,
    preferences: KiebordPreferences,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSettingsColors.current

    val currentScheme = when (preferences.theme) {
        KiebordPreferences.THEME_AMOLED_BLACK -> KeyboardColorScheme.AmoledBlack
        KiebordPreferences.THEME_TOKYO_NIGHT -> KeyboardColorScheme.TokyoNight
        KiebordPreferences.THEME_NORD -> KeyboardColorScheme.Nord
        KiebordPreferences.THEME_DEFAULT_LIGHT -> KeyboardColorScheme.DefaultLight
        else -> KeyboardColorScheme.DefaultDark
    }

    val itemWidth = 164.dp
    val thumbnailHeight = 120.dp

    val pillBg = if (isEnabled) {
        if (colors.isDark) Color(0xFFC3C8BC) else Color(0xFFD6E3D0)
    } else {
        Color.Transparent
    }
    val pillContentColor = if (isEnabled) {
        Color(0xFF283125)
    } else {
        colors.textSecondary
    }

    Column(
        modifier = modifier
            .width(itemWidth)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Keyboard Preview Thumbnail (portrait phone screen aspect with keyboard centered)
        Box(
            modifier = Modifier
                .width(itemWidth)
                .height(thumbnailHeight)
                .clip(RoundedCornerShape(14.dp))
                .background(currentScheme.containerBackground)
                .border(
                    width = if (isEnabled) 1.5.dp else 1.dp,
                    color = if (isEnabled) colors.accent.copy(alpha = 0.5f) else colors.cardBorder,
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            val virtualWidth = 336.dp
            val virtualHeight = 206.dp
            val scale = 164f / 336f

            KiebordTheme(colorScheme = currentScheme) {
                Box(
                    modifier = Modifier
                        .requiredSize(virtualWidth, virtualHeight)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    KeyboardRootView(
                        layout = cardData.layoutDef,
                        shiftState = ShiftState.UNSHIFTED,
                        onKeyTap = {},
                        onKeyLongPress = {},
                        showKeyBorders = true,
                        defaultShowNumberRow = false,
                        defaultShowSecondaryKeys = false,
                        initialRowHeight = 42.dp,
                        applySystemInsets = false,
                        isInteractive = false,
                        navBarBottomInset = 0.dp
                    )
                }
            }

            // Transparent overlay interceptor so touch doesn't pass to keyboard internals
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggle
                    )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Selection Pill (capsule with checkmark or hollow circle)
        Box(
            modifier = Modifier
                .width(itemWidth)
                .height(34.dp)
                .clip(CircleShape)
                .background(pillBg)
                .then(
                    if (!isEnabled) {
                        Modifier.border(1.dp, colors.inputBorder, CircleShape)
                    } else Modifier
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggle
                )
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isEnabled) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Enabled",
                        tint = pillContentColor,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .border(1.5.dp, pillContentColor.copy(alpha = 0.6f), CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = cardData.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = pillContentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 3. Subtitle Row with Badge and Description
        if (cardData.description.isNotEmpty() || cardData.badge.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.width(itemWidth),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (cardData.badge.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.iconBackground)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = cardData.badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                    }
                }
                if (cardData.badge.isNotEmpty() && cardData.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(5.dp))
                }
                if (cardData.description.isNotEmpty()) {
                    Text(
                        text = cardData.description,
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
