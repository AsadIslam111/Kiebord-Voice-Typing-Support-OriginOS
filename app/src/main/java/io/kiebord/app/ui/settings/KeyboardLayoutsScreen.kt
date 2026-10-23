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
            onToggle = {
                if (!isEnglishLastRemaining || !isEnglishEnabled) {
                    onToggleLayoutEnabled(englishCard.id, !isEnglishEnabled)
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Bangla Language Group Card (Expandable into separate real keyboard cards)
        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
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
                            .background(Color(0xFF2B2D31)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "বাং",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7AA2F7)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bangla Language",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE0E2E8)
                        )
                        Text(
                            text = if (isBanglaExpanded) "Borno Phonetic • Probhat" else "$activeBanglaCount active layout(s) • tap to expand",
                            fontSize = 12.sp,
                            color = Color(0xFF9499A8)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (activeBanglaCount > 0) Color(0xFF233047) else Color(0xFF25272B))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isBanglaExpanded) "Expanded" else "$activeBanglaCount Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (activeBanglaCount > 0) Color(0xFF7AA2F7) else Color(0xFF9499A8)
                        )
                    }
                }

                // Expanded Section: Separate Real Keyboard Cards with Checkboxes
                AnimatedVisibility(
                    visible = isBanglaExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        banglaCards.forEach { cardData ->
                            val isEnabled = preferences.enabledLayoutIds.contains(cardData.id)
                            val isLastRemaining = preferences.enabledLayoutIds.size == 1 && isEnabled

                            RealKeyboardPreviewCard(
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

@Composable
private fun RealKeyboardPreviewCard(
    cardData: LayoutCardData,
    preferences: KiebordPreferences,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isEnabled) Color(0xFF7AA2F7) else Color(0xFF2A2D33)
    val cardBackground = Color(0xFF14161A)

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
            .clip(RoundedCornerShape(14.dp))
            .background(cardBackground)
            .border(
                width = if (isEnabled) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
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
                    .background(Color(0xFF22252B))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = cardData.badge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7AA2F7)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cardData.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isEnabled) Color(0xFF7AA2F7) else Color(0xFFE0E2E8)
                )
                Text(
                    text = cardData.description,
                    fontSize = 11.sp,
                    color = Color(0xFF9499A8),
                    maxLines = 1
                )
            }

            Checkbox(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF7AA2F7),
                    uncheckedColor = Color(0xFF6B7280),
                    checkmarkColor = Color(0xFF0F141C)
                )
            )
        }
    }
}
