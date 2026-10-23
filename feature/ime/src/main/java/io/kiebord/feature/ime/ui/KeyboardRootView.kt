package io.kiebord.feature.ime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kiebord.common.model.KeyDef
import io.kiebord.common.model.KeyType
import io.kiebord.common.model.KeyboardLayoutDef
import io.kiebord.common.model.ShiftState
import io.kiebord.theme.KiebordTheme

@Composable
fun KeyboardRootView(
    layout: KeyboardLayoutDef,
    shiftState: ShiftState,
    modifier: Modifier = Modifier,
    keyRowHeight: Dp = 60.dp,
    onKeyTap: (KeyDef) -> Unit,
    onKeyLongPress: (KeyDef) -> Unit = {}
) {
    val colorScheme = KiebordTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.containerBackground)
            .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 8.dp)
    ) {
        // Layout key rows
        layout.rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(keyRowHeight)
            ) {
                row.keys.forEach { key ->
                    val effectiveKey = if (key.type == KeyType.SPACE && key.label.isBlank()) {
                        val spaceLabel = when {
                            layout.id.startsWith("symbols") -> "Space"
                            layout.language == "bn" -> "বাংলা"
                            else -> "English"
                        }
                        key.copy(label = spaceLabel)
                    } else {
                        key
                    }

                    KeycapView(
                        key = effectiveKey,
                        shiftState = shiftState,
                        modifier = Modifier
                            .weight(effectiveKey.widthWeight)
                            .fillMaxHeight(),
                        onKeyTap = onKeyTap,
                        onKeyLongPress = onKeyLongPress
                    )
                }
            }
        }
    }
}
