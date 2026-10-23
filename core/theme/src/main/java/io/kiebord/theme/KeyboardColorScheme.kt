package io.kiebord.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class KeyboardColorScheme(
    val containerBackground: Color,
    val keyBackground: Color,
    val keyForeground: Color,
    val keyBorder: Color,
    val keyShadow: Color,
    val actionKeyBackground: Color,
    val actionKeyForeground: Color,
    val shiftActiveColor: Color,
    val accentColor: Color,
    val moreKeysBackground: Color,
    val guidelineColor: Color,
    val suggestionBackground: Color,
    val suggestionForeground: Color
) {
    companion object {
        val DefaultDark = KeyboardColorScheme(
            containerBackground = Color(0xFF1E1F22),
            keyBackground = Color(0xFF2B2D31),
            keyForeground = Color(0xFFE0E2E8),
            keyBorder = Color(0xFF383A40),
            keyShadow = Color(0x33000000),
            actionKeyBackground = Color(0xFF3B4048),
            actionKeyForeground = Color(0xFFFFFFFF),
            shiftActiveColor = Color(0xFF7AA2F7),
            accentColor = Color(0xFF7AA2F7),
            moreKeysBackground = Color(0xFF25272B),
            guidelineColor = Color(0xFF7AA2F7),
            suggestionBackground = Color(0xFF1E1F22),
            suggestionForeground = Color(0xFFE0E2E8)
        )

        val AmoledBlack = KeyboardColorScheme(
            containerBackground = Color(0xFF000000),
            keyBackground = Color(0xFF121212),
            keyForeground = Color(0xFFFFFFFF),
            keyBorder = Color(0xFF222222),
            keyShadow = Color(0x00000000),
            actionKeyBackground = Color(0xFF1E1E1E),
            actionKeyForeground = Color(0xFF64B5F6),
            shiftActiveColor = Color(0xFF64B5F6),
            accentColor = Color(0xFF64B5F6),
            moreKeysBackground = Color(0xFF181818),
            guidelineColor = Color(0xFF64B5F6),
            suggestionBackground = Color(0xFF000000),
            suggestionForeground = Color(0xFFFFFFFF)
        )

        val DefaultLight = KeyboardColorScheme(
            containerBackground = Color(0xFFECEFF1),
            keyBackground = Color(0xFFFFFFFF),
            keyForeground = Color(0xFF263238),
            keyBorder = Color(0xFFCFD8DC),
            keyShadow = Color(0x1F000000),
            actionKeyBackground = Color(0xFFCFD8DC),
            actionKeyForeground = Color(0xFF263238),
            shiftActiveColor = Color(0xFF1976D2),
            accentColor = Color(0xFF1976D2),
            moreKeysBackground = Color(0xFFFFFFFF),
            guidelineColor = Color(0xFF1976D2),
            suggestionBackground = Color(0xFFECEFF1),
            suggestionForeground = Color(0xFF263238)
        )
    }
}
