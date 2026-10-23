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
            containerBackground = Color(0xFF1B1B1F),
            keyBackground = Color(0xFF2C2C30),
            keyForeground = Color(0xFFE3E2E6),
            keyBorder = Color(0x1FFFFFFF),
            keyShadow = Color(0x28000000),
            actionKeyBackground = Color(0xFF242428),
            actionKeyForeground = Color(0xFF04294E),
            shiftActiveColor = Color(0xFFA8C7FA),
            accentColor = Color(0xFFA8C7FA),
            moreKeysBackground = Color(0xFF252529),
            guidelineColor = Color(0xFFA8C7FA),
            suggestionBackground = Color(0xFF1B1B1F),
            suggestionForeground = Color(0xFFE3E2E6)
        )

        val AmoledBlack = KeyboardColorScheme(
            containerBackground = Color(0xFF000000),
            keyBackground = Color(0xFF18181B),
            keyForeground = Color(0xFFFFFFFF),
            keyBorder = Color(0xFF222225),
            keyShadow = Color(0x00000000),
            actionKeyBackground = Color(0xFF111113),
            actionKeyForeground = Color(0xFF04294E),
            shiftActiveColor = Color(0xFFA8C7FA),
            accentColor = Color(0xFFA8C7FA),
            moreKeysBackground = Color(0xFF18181B),
            guidelineColor = Color(0xFFA8C7FA),
            suggestionBackground = Color(0xFF000000),
            suggestionForeground = Color(0xFFFFFFFF)
        )

        val DefaultLight = KeyboardColorScheme(
            containerBackground = Color(0xFFF1F3F4),
            keyBackground = Color(0xFFFFFFFF),
            keyForeground = Color(0xFF1F1F1F),
            keyBorder = Color(0xFFD3D6DA),
            keyShadow = Color(0x22000000),
            actionKeyBackground = Color(0xFFE2E4E8),
            actionKeyForeground = Color(0xFFFFFFFF),
            shiftActiveColor = Color(0xFF0B57D0),
            accentColor = Color(0xFF0B57D0),
            moreKeysBackground = Color(0xFFFFFFFF),
            guidelineColor = Color(0xFF0B57D0),
            suggestionBackground = Color(0xFFF1F3F4),
            suggestionForeground = Color(0xFF1F1F1F)
        )

        val TokyoNight = KeyboardColorScheme(
            containerBackground = Color(0xFF1A1B26),
            keyBackground = Color(0xFF24283B),
            keyForeground = Color(0xFFC0CAF5),
            keyBorder = Color(0xFF292E42),
            keyShadow = Color(0x22000000),
            actionKeyBackground = Color(0xFF1F2335),
            actionKeyForeground = Color(0xFF1A1B26),
            shiftActiveColor = Color(0xFF7AA2F7),
            accentColor = Color(0xFF7AA2F7),
            moreKeysBackground = Color(0xFF1F2335),
            guidelineColor = Color(0xFF7AA2F7),
            suggestionBackground = Color(0xFF1A1B26),
            suggestionForeground = Color(0xFFC0CAF5)
        )

        val Nord = KeyboardColorScheme(
            containerBackground = Color(0xFF2E3440),
            keyBackground = Color(0xFF3B4252),
            keyForeground = Color(0xFFECEFF4),
            keyBorder = Color(0xFF434C5E),
            keyShadow = Color(0x22000000),
            actionKeyBackground = Color(0xFF353B49),
            actionKeyForeground = Color(0xFF2E3440),
            shiftActiveColor = Color(0xFF88C0D0),
            accentColor = Color(0xFF88C0D0),
            moreKeysBackground = Color(0xFF2E3440),
            guidelineColor = Color(0xFF88C0D0),
            suggestionBackground = Color(0xFF2E3440),
            suggestionForeground = Color(0xFFECEFF4)
        )
    }
}

