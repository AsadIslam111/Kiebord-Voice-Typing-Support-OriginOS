package io.kiebord.feature.ime.ui

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
import io.kiebord.common.model.ShiftState

@Composable
fun NumberRowBar(
    language: String,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    showKeyBorders: Boolean = true,
    isInteractive: Boolean = true,
    onKeyTap: (KeyDef) -> Unit,
    onKeyLongPress: (KeyDef) -> Unit = {}
) {
    val numberKeys = if (language == "bn") {
        listOf(
            KeyDef("১", flickUp = "1", moreKeys = listOf("1", "১")),
            KeyDef("২", flickUp = "2", moreKeys = listOf("2", "২")),
            KeyDef("৩", flickUp = "3", moreKeys = listOf("3", "৩")),
            KeyDef("৪", flickUp = "4", moreKeys = listOf("4", "৪")),
            KeyDef("৫", flickUp = "5", moreKeys = listOf("5", "৫")),
            KeyDef("৬", flickUp = "6", moreKeys = listOf("6", "৬")),
            KeyDef("৭", flickUp = "7", moreKeys = listOf("7", "৭")),
            KeyDef("৮", flickUp = "8", moreKeys = listOf("8", "৮")),
            KeyDef("৯", flickUp = "9", moreKeys = listOf("9", "৯")),
            KeyDef("০", flickUp = "0", moreKeys = listOf("0", "০"))
        )
    } else {
        listOf(
            KeyDef("1", flickUp = "¹", moreKeys = listOf("1", "¹", "½", "১")),
            KeyDef("2", flickUp = "²", moreKeys = listOf("2", "²", "⅔", "২")),
            KeyDef("3", flickUp = "³", moreKeys = listOf("3", "³", "¾", "৩")),
            KeyDef("4", flickUp = "⁴", moreKeys = listOf("4", "⁴", "৪")),
            KeyDef("5", flickUp = "⁵", moreKeys = listOf("5", "⁵", "৫")),
            KeyDef("6", flickUp = "⁶", moreKeys = listOf("6", "⁶", "৬")),
            KeyDef("7", flickUp = "⁷", moreKeys = listOf("7", "⁷", "৭")),
            KeyDef("8", flickUp = "⁸", moreKeys = listOf("8", "⁸", "৮")),
            KeyDef("9", flickUp = "⁹", moreKeys = listOf("9", "⁹", "৯")),
            KeyDef("0", flickUp = "⁰", moreKeys = listOf("0", "⁰", "০", "∅"))
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(bottom = 2.dp)
    ) {
        numberKeys.forEach { key ->
            KeycapView(
                key = key,
                shiftState = ShiftState.UNSHIFTED,
                showKeyBorders = showKeyBorders,
                isInteractive = isInteractive,
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight(),
                onKeyTap = onKeyTap,
                onKeyLongPress = onKeyLongPress
            )
        }
    }
}
