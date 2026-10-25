package io.kiebord.app.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.app.R

@Immutable
data class SettingsColors(
    val background: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val iconBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val sectionTitle: Color,
    val divider: Color,
    val inputBackground: Color,
    val inputBorder: Color,
    val dialogBackground: Color,
    val warningCardBg: Color,
    val warningCardText: Color,
    val warningCardSubtext: Color,
    val warningBtnBg: Color,
    val warningBtnText: Color,
    val activeCardBg: Color,
    val activeCardText: Color,
    val activeCardSubtext: Color,
    val activeCardIcon: Color,
    val isDark: Boolean
)

val DarkSettingsColors = SettingsColors(
    background = Color(0xFF131417),
    cardBackground = Color(0xFF1E1F22),
    cardBorder = Color(0x1AFFFFFF),
    iconBackground = Color(0xFF2B2D31),
    textPrimary = Color(0xFFE0E2E8),
    textSecondary = Color(0xFF9499A8),
    accent = Color(0xFFFF9E55),
    sectionTitle = Color(0xFFFFFFFF),
    divider = Color(0xFF2B2D31),
    inputBackground = Color(0xFF25272B),
    inputBorder = Color(0xFF383A40),
    dialogBackground = Color(0xFF1E1F22),
    warningCardBg = Color(0xFF2B271F),
    warningCardText = Color(0xFFFFB86C),
    warningCardSubtext = Color(0xFFD4C8B8),
    warningBtnBg = Color(0xFFFFB86C),
    warningBtnText = Color(0xFF1E1F22),
    activeCardBg = Color(0xFF1B2E20),
    activeCardText = Color(0xFFA5D6A7),
    activeCardSubtext = Color(0xFF81C784),
    activeCardIcon = Color(0xFF81C784),
    isDark = true
)

val LightSettingsColors = SettingsColors(
    background = Color(0xFFF2F4F7),
    cardBackground = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFE5E7EB),
    iconBackground = Color(0xFFEEF2F6),
    textPrimary = Color(0xFF111827),
    textSecondary = Color(0xFF6B7280),
    accent = Color(0xFF9C4300),
    sectionTitle = Color(0xFF9C4300),
    divider = Color(0xFFE5E7EB),
    inputBackground = Color(0xFFF9FAFB),
    inputBorder = Color(0xFFD1D5DB),
    dialogBackground = Color(0xFFFFFFFF),
    warningCardBg = Color(0xFFFFFBEB),
    warningCardText = Color(0xFFB45309),
    warningCardSubtext = Color(0xFF92400E),
    warningBtnBg = Color(0xFFF59E0B),
    warningBtnText = Color(0xFFFFFFFF),
    activeCardBg = Color(0xFFECFDF5),
    activeCardText = Color(0xFF065F46),
    activeCardSubtext = Color(0xFF047857),
    activeCardIcon = Color(0xFF10B981),
    isDark = false
)

val LocalSettingsColors = staticCompositionLocalOf { DarkSettingsColors }

@Composable
fun SettingsUnifiedTopBar(
    screen: SettingsScreen,
    onBack: () -> Unit
) {
    val colors = LocalSettingsColors.current
    val title = when (screen) {
        is SettingsScreen.Main -> "Kiebord"
        is SettingsScreen.AppearanceThemes -> "Appearance & Themes"
        is SettingsScreen.KeyboardLayouts -> "Keyboard Layouts"
        is SettingsScreen.Preferences -> "Preferences"
        is SettingsScreen.CorrectionsSuggestions -> "Corrections & Suggestions"
        is SettingsScreen.Clipboard -> "Clipboard"
        is SettingsScreen.About -> "About Kiebord"
        is SettingsScreen.Onboarding -> "Setup Wizard"
    }

    val subtitle: String? = when (screen) {
        is SettingsScreen.Main -> null
        is SettingsScreen.AppearanceThemes -> "Themes & key styling"
        is SettingsScreen.KeyboardLayouts -> "Active languages & preview"
        is SettingsScreen.Preferences -> "Haptics, smart gestures & ergonomics"
        is SettingsScreen.CorrectionsSuggestions -> "Candidate strip, auto-fix & glide typing"
        is SettingsScreen.Clipboard -> "Manage copied items & pins"
        is SettingsScreen.About -> "Privacy, engine architecture & diagnostics"
        is SettingsScreen.Onboarding -> "Welcome to Kiebord"
    }

    val isMain = screen == SettingsScreen.Main

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = isMain,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
            label = "TopBarIconTransition"
        ) { mainState ->
            if (mainState) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher),
                    contentDescription = "Kiebord Logo",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(9.dp))
                )
            } else {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        AnimatedContent(
            targetState = title to subtitle,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
            label = "TopBarTitleTransition",
            modifier = Modifier.weight(1f)
        ) { (t, s) ->
            Column {
                Text(
                    text = t,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                if (!s.isNullOrBlank()) {
                    Text(
                        text = s,
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsTopAppBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit
) {
    val colors = LocalSettingsColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun SettingsCategoryHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalSettingsColors.current
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.sectionTitle,
        modifier = modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
    )
}

enum class GroupPosition {
    TOP,
    MIDDLE,
    BOTTOM,
    ALONE
}

fun getGroupItemShape(
    position: GroupPosition,
    outerRadius: Dp = 20.dp,
    innerRadius: Dp = 4.dp
): RoundedCornerShape {
    return when (position) {
        GroupPosition.TOP -> RoundedCornerShape(
            topStart = outerRadius,
            topEnd = outerRadius,
            bottomStart = innerRadius,
            bottomEnd = innerRadius
        )
        GroupPosition.MIDDLE -> RoundedCornerShape(innerRadius)
        GroupPosition.BOTTOM -> RoundedCornerShape(
            topStart = innerRadius,
            topEnd = innerRadius,
            bottomStart = outerRadius,
            bottomEnd = outerRadius
        )
        GroupPosition.ALONE -> RoundedCornerShape(outerRadius)
    }
}

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    spacing: Dp = 3.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalSettingsColors.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsItemIcon(
    icon: ImageVector? = null,
    @DrawableRes iconResId: Int? = null,
    iconTint: Color,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp
) {
    if (icon == null && iconResId == null) return

    val colors = LocalSettingsColors.current
    val isAppIcon = iconResId == R.drawable.ic_launcher || iconResId == R.drawable.ic_launcher_foreground
    val isKeyboardIcon = icon == Icons.Outlined.Keyboard

    if (isAppIcon) {
        Image(
            painter = painterResource(iconResId!!),
            contentDescription = null,
            modifier = Modifier.size(size)
        )
    } else if (isKeyboardIcon) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon!!,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(colors.iconBackground),
            contentAlignment = Alignment.Center
        ) {
            if (iconResId != null) {
                Icon(
                    painter = painterResource(iconResId),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(iconSize)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

@Composable
fun SettingsNavigationItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    @DrawableRes iconResId: Int? = null,
    iconTint: Color? = null,
    titleFontWeight: FontWeight = FontWeight.Bold,
    badge: String? = null,
    position: GroupPosition? = null,
    showDivider: Boolean = true,
    showChevron: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalSettingsColors.current
    val effectiveTint = iconTint ?: colors.textSecondary

    if (position != null) {
        val shape = remember(position) { getGroupItemShape(position) }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null || iconResId != null) {
                    SettingsItemIcon(
                        icon = icon,
                        iconResId = iconResId,
                        iconTint = effectiveTint,
                        size = 40.dp,
                        iconSize = 22.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = titleFontWeight,
                            color = colors.textPrimary
                        )
                        if (badge != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.accent.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent
                                )
                            }
                        }
                    }
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                if (showChevron) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.textSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    } else {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null || iconResId != null) {
                    SettingsItemIcon(
                        icon = icon,
                        iconResId = iconResId,
                        iconTint = effectiveTint,
                        size = 40.dp,
                        iconSize = 22.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = titleFontWeight,
                            color = colors.textPrimary
                        )
                        if (badge != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.accent.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent
                                )
                            }
                        }
                    }
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                if (showChevron) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.textSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                    thickness = 0.5.dp,
                    color = colors.divider
                )
            }
        }
    }
}

@Composable
fun SettingsItemCard(
    position: GroupPosition = GroupPosition.ALONE,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = LocalSettingsColors.current
    val shape = remember(position) { getGroupItemShape(position) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        content()
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    @DrawableRes iconResId: Int? = null,
    iconTint: Color? = null,
    checked: Boolean,
    position: GroupPosition? = null,
    showDivider: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalSettingsColors.current
    val effectiveTint = iconTint ?: colors.textSecondary

    if (position != null) {
        val shape = remember(position) { getGroupItemShape(position) }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCheckedChange(!checked) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null || iconResId != null) {
                    SettingsItemIcon(
                        icon = icon,
                        iconResId = iconResId,
                        iconTint = effectiveTint,
                        size = 40.dp,
                        iconSize = 22.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.textSecondary,
                        uncheckedTrackColor = colors.iconBackground,
                        uncheckedBorderColor = colors.inputBorder
                    )
                )
            }
        }
    } else {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCheckedChange(!checked) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null || iconResId != null) {
                    SettingsItemIcon(
                        icon = icon,
                        iconResId = iconResId,
                        iconTint = effectiveTint,
                        size = 36.dp,
                        iconSize = 20.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.textSecondary,
                        uncheckedTrackColor = colors.iconBackground,
                        uncheckedBorderColor = colors.inputBorder
                    )
                )
            }

            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = colors.divider
                )
            }
        }
    }
}

@Composable
fun SettingsSliderItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    @DrawableRes iconResId: Int? = null,
    iconTint: Color? = null,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    valueLabel: String,
    position: GroupPosition? = null,
    showDivider: Boolean = true,
    onValueChange: (Float) -> Unit
) {
    val colors = LocalSettingsColors.current
    val effectiveTint = iconTint ?: colors.textSecondary

    if (position != null) {
        val shape = remember(position) { getGroupItemShape(position) }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null || iconResId != null) {
                        SettingsItemIcon(
                            icon = icon,
                            iconResId = iconResId,
                            iconTint = effectiveTint,
                            size = 40.dp,
                            iconSize = 22.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.iconBackground)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = valueLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    steps = steps,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accent,
                        activeTrackColor = colors.accent,
                        inactiveTrackColor = colors.iconBackground
                    )
                )
            }
        }
    } else {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null || iconResId != null) {
                        SettingsItemIcon(
                            icon = icon,
                            iconResId = iconResId,
                            iconTint = effectiveTint,
                            size = 36.dp,
                            iconSize = 20.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.iconBackground)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = valueLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    steps = steps,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accent,
                        activeTrackColor = colors.accent,
                        inactiveTrackColor = colors.iconBackground
                    )
                )
            }

            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = colors.divider
                )
            }
        }
    }
}

@Composable
fun SettingsRadioOption(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    position: GroupPosition? = null,
    showDivider: Boolean = true,
    onSelect: () -> Unit
) {
    val colors = LocalSettingsColors.current

    if (position != null) {
        val shape = remember(position) { getGroupItemShape(position) }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSelect)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                RadioButton(
                    selected = selected,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = colors.accent,
                        unselectedColor = colors.textSecondary
                    )
                )
            }
        }
    } else {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSelect)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                RadioButton(
                    selected = selected,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = colors.accent,
                        unselectedColor = colors.textSecondary
                    )
                )
            }

            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = colors.divider
                )
            }
        }
    }
}
