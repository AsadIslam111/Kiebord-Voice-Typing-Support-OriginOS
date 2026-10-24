package io.kiebord.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.text.font.FontWeight
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        SettingsCategoryHeader(title = "Available Keyboard Layouts")

        // 1. English (QWERTY) Real Keyboard Card
        val isEnglishEnabled = preferences.enabledLayoutIds.contains(englishCard.id)
        val isEnglishLastRemaining = preferences.enabledLayoutIds.size == 1 && isEnglishEnabled

        RealKeyboardPreviewCard(
            cardData = englishCard,
            preferences = preferences,
            isEnabled = isEnglishEnabled,
            position = GroupPosition.ALONE,
            onToggle = {
                if (!isEnglishLastRemaining || !isEnglishEnabled) {
                    onToggleLayoutEnabled(englishCard.id, !isEnglishEnabled)
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Bangla Language Group Card (Expandable into separate real keyboard cards)
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
                        if (colors.isDark) Color(0xFF233047) else Color(0xFFEBF2FE)
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

                // Expanded Section: Separate Real Keyboard Cards with Checkboxes & GroupPosition radii
                AnimatedVisibility(
                    visible = isBanglaExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        banglaCards.forEachIndexed { index, cardData ->
                            val isEnabled = preferences.enabledLayoutIds.contains(cardData.id)
                            val isLastRemaining = preferences.enabledLayoutIds.size == 1 && isEnabled
                            val itemPos = when (index) {
                                0 -> GroupPosition.TOP
                                banglaCards.lastIndex -> GroupPosition.BOTTOM
                                else -> GroupPosition.MIDDLE
                            }

                            RealKeyboardPreviewCard(
                                cardData = cardData,
                                preferences = preferences,
                                isEnabled = isEnabled,
                                position = itemPos,
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

@Composable
private fun RealKeyboardPreviewCard(
    cardData: LayoutCardData,
    preferences: KiebordPreferences,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    position: GroupPosition = GroupPosition.ALONE,
    modifier: Modifier = Modifier
) {
    val colors = LocalSettingsColors.current
    val borderColor = if (isEnabled) colors.accent else colors.cardBorder
    val cardBackground = colors.cardBackground
    val shape = remember(position) { getGroupItemShape(position) }

    val currentScheme = when (preferences.theme) {
        KiebordPreferences.THEME_AMOLED_BLACK -> KeyboardColorScheme.AmoledBlack
        KiebordPreferences.THEME_TOKYO_NIGHT -> KeyboardColorScheme.TokyoNight
        KiebordPreferences.THEME_NORD -> KeyboardColorScheme.Nord
        KiebordPreferences.THEME_DEFAULT_LIGHT -> KeyboardColorScheme.DefaultLight
        else -> KeyboardColorScheme.DefaultDark
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBackground)
            .border(
                width = if (isEnabled) 1.5.dp else 1.dp,
                color = borderColor,
                shape = shape
            )
            .clickable(onClick = onToggle)
            .padding(12.dp)
    ) {
        // Real Keyboard Surface View with corresponding shift, delete, enter icons
        KiebordTheme(colorScheme = currentScheme) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(currentScheme.containerBackground)
            ) {
                KeyboardRootView(
                    layout = cardData.layoutDef,
                    shiftState = ShiftState.UNSHIFTED,
                    onKeyTap = {},
                    onKeyLongPress = {},
                    showKeyBorders = preferences.showKeyBorders,
                    defaultShowNumberRow = false,
                    defaultShowSecondaryKeys = false,
                    initialRowHeight = 36.dp,
                    applySystemInsets = false,
                    isInteractive = false,
                    navBarBottomInset = 0.dp
                )

                // Transparent interceptor layer so the layout keyboard preview is completely non-touchable
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
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom info row with badge, name, description, and checkbox
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.iconBackground)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = cardData.badge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cardData.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isEnabled) colors.accent else colors.textPrimary
                )
                Text(
                    text = cardData.description,
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    maxLines = 1
                )
            }

            Checkbox(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = colors.accent,
                    uncheckedColor = colors.textSecondary,
                    checkmarkColor = if (colors.isDark) Color(0xFF0F141C) else Color(0xFFFFFFFF)
                )
            )
        }
    }
}
