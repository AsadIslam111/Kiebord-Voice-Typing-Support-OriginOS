package io.kiebord.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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

    val englishCard = LayoutCardData(
        id = KiebordPreferences.LAYOUT_EN_QWERTY,
        name = "English (QWERTY)",
        badge = "EN",
        description = "Standard Latin layout with gesture typing",
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
            description = "Fixed layout",
            layoutDef = PredefinedLayouts.BengaliProbhat
        ),
        LayoutCardData(
            id = KiebordPreferences.LAYOUT_BN_NATIONAL,
            name = "National (Jatiyo)",
            badge = "জাতীয়",
            description = "National BSTI layout",
            layoutDef = PredefinedLayouts.BengaliNational
        )
    )

    val isEnglishEnabled = preferences.enabledLayoutIds.contains(englishCard.id)
    val isEnglishLastRemaining = preferences.enabledLayoutIds.size == 1 && isEnglishEnabled

    val isAnyBanglaEnabled = preferences.enabledLayoutIds.any { it.startsWith("bn") }
    var isEnglishExpanded by remember { mutableStateOf(isEnglishEnabled) }
    var isBanglaExpanded by remember { mutableStateOf(isAnyBanglaEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        SettingsCategoryHeader(title = "Available Keyboard Layouts")

        SettingsGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            // 1. English Item
            SettingsItemCard(
                position = GroupPosition.TOP,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isEnglishExpanded = !isEnglishExpanded }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "English",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Standard Latin layout with gesture typing",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }

                        val chevronRotation by animateFloatAsState(
                            targetValue = if (isEnglishExpanded) 90f else 0f,
                            label = "EnglishChevronRotation"
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = if (isEnglishExpanded) "Collapse" else "Expand",
                            tint = colors.textSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = chevronRotation }
                        )
                    }

                    AnimatedVisibility(
                        visible = isEnglishExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
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
            }

            // 2. Bangla Item with Horizontal Carousel
            SettingsItemCard(
                position = GroupPosition.BOTTOM,
                modifier = Modifier.fillMaxWidth()
            ) {
                val activeBanglaCount = banglaCards.count { preferences.enabledLayoutIds.contains(it.id) }

                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isBanglaExpanded = !isBanglaExpanded }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bangla",
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

                        val chevronRotation by animateFloatAsState(
                            targetValue = if (isBanglaExpanded) 90f else 0f,
                            label = "BanglaChevronRotation"
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = if (isBanglaExpanded) "Collapse" else "Expand",
                            tint = colors.textSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = chevronRotation }
                        )
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

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Selection Row: Layout Name without pill shape
        Row(
            modifier = Modifier.width(itemWidth),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isEnabled) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Enabled",
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .border(1.5.dp, colors.textSecondary.copy(alpha = 0.6f), CircleShape)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }

            Text(
                text = cardData.name,
                fontSize = 13.sp,
                fontWeight = if (isEnabled) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isEnabled) colors.textPrimary else colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 3. Subtitle Row with Description
        if (cardData.description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(3.dp))
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
