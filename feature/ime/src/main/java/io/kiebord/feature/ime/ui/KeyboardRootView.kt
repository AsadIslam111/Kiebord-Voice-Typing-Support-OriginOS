package io.kiebord.feature.ime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kiebord.common.model.KeyDef
import io.kiebord.common.model.KeyboardLayoutDef
import io.kiebord.common.model.ShiftState
import io.kiebord.theme.KiebordTheme

@Composable
fun KeyboardRootView(
    layout: KeyboardLayoutDef,
    shiftState: ShiftState,
    modifier: Modifier = Modifier,
    onKeyTap: (KeyDef) -> Unit,
    onKeyLongPress: (KeyDef) -> Unit = {}
) {
    val colorScheme = KiebordTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.containerBackground)
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        // Layout key rows
        layout.rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                row.keys.forEach { key ->
                    KeycapView(
                        key = key,
                        shiftState = shiftState,
                        modifier = Modifier.weight(key.widthWeight),
                        onKeyTap = onKeyTap,
                        onKeyLongPress = onKeyLongPress
                    )
                }
            }
        }
    }
}
