package io.kiebord.feature.ime.ui.dialog

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.data.layout.PredefinedLayouts
import io.kiebord.theme.KiebordTheme

@Composable
fun LanguageSelectionDialog(
    currentLayoutId: String,
    enabledLayoutIds: List<String>,
    onSelectLayout: (String) -> Unit,
    onMoreLayouts: () -> Unit,
    onOtherKeyboards: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = KiebordTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 250.dp, max = 290.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colorScheme.containerBackground)
                .border(
                    width = 1.dp,
                    color = colorScheme.keyForeground.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Consume clicks to prevent dismissing dialog */ }
                .padding(vertical = 14.dp, horizontal = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = null,
                    tint = colorScheme.accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Select Layout",
                    color = colorScheme.keyForeground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = KiebordTheme.fontFamily
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = colorScheme.keyForeground.copy(alpha = 0.12f),
                thickness = 0.5.dp
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Enabled Layouts list
            val layoutsToShow = enabledLayoutIds.ifEmpty { listOf("en_qwerty") }
            layoutsToShow.forEach { layoutId ->
                val isSelected = layoutId == currentLayoutId
                val displayName = PredefinedLayouts.displayName(layoutId)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectLayout(layoutId)
                            onDismiss()
                        }
                        .padding(horizontal = 8.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = displayName,
                        color = if (isSelected) colorScheme.accentColor else colorScheme.keyForeground,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        fontFamily = KiebordTheme.fontFamily
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(colorScheme.accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Active",
                                tint = colorScheme.accentColor,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(
                color = colorScheme.keyForeground.copy(alpha = 0.12f),
                thickness = 0.5.dp
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Action: More Layouts...
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        onDismiss()
                        onMoreLayouts()
                    }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = colorScheme.keyForeground.copy(alpha = 0.75f),
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "More Layouts...",
                    color = colorScheme.keyForeground.copy(alpha = 0.88f),
                    fontSize = 13.5.sp,
                    fontFamily = KiebordTheme.fontFamily
                )
            }

            // Action: Other Keyboards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        onDismiss()
                        onOtherKeyboards()
                    }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Keyboard,
                    contentDescription = null,
                    tint = colorScheme.keyForeground.copy(alpha = 0.75f),
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Other Keyboards",
                    color = colorScheme.keyForeground.copy(alpha = 0.88f),
                    fontSize = 13.5.sp,
                    fontFamily = KiebordTheme.fontFamily
                )
            }
        }
    }
}
