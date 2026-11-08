package com.codepotro.borno.feature.ime.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codepotro.borno.common.model.KeyDef
import com.codepotro.borno.common.model.ShiftState
import com.codepotro.borno.data.layout.PredefinedLayouts

@Composable
fun NumberRowBar(
    language: String,
    modifier: Modifier = Modifier,
    height: Dp = 50.dp,
    rowHeight: Dp = height,
    showKeyBorders: Boolean = true,
    showKeyPopups: Boolean = true,
    isInteractive: Boolean = true,
    onKeyPress: (KeyDef) -> Unit = {},
    onKeyTap: (KeyDef) -> Unit,
    onKeyLongPress: (KeyDef) -> Unit = {},
    onKeyPositioned: (KeyDef, LayoutCoordinates) -> Unit = { _, _ -> },
    onShowMoreKeys: (KeyDef, LayoutCoordinates) -> Unit = { _, _ -> },
    onMoreKeysHover: (LayoutCoordinates, Offset) -> Unit = { _, _ -> },
    onMoreKeysCommit: () -> Unit = {}
) {
    val numberKeys = PredefinedLayouts.createNumberRow(language).keys

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(bottom = 2.dp)
    ) {
        numberKeys.forEachIndexed { keyIndex, key ->
            KeycapView(
                key = key,
                shiftState = ShiftState.UNSHIFTED,
                rowIndex = -1,
                keyIndex = keyIndex,
                showKeyBorders = showKeyBorders,
                showKeyPopups = showKeyPopups,
                showSecondaryKeys = false,
                rowHeight = rowHeight,
                scaleFactor = (rowHeight.value / 50f).coerceIn(0.4f, 1.6f),
                isInteractive = isInteractive,
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight(),
                onKeyPress = onKeyPress,
                onKeyTap = onKeyTap,
                onKeyLongPress = onKeyLongPress,
                onKeyPositioned = onKeyPositioned,
                onShowMoreKeys = onShowMoreKeys,
                onMoreKeysHover = onMoreKeysHover,
                onMoreKeysCommit = onMoreKeysCommit
            )
        }
    }
}
