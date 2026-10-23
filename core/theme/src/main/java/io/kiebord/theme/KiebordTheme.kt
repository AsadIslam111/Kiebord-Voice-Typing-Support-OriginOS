package io.kiebord.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.kiebord.core.theme.R

val KeyboardFontFamily = FontFamily(
    Font(R.font.keyboard_font, FontWeight.Normal),
    Font(R.font.keyboard_font, FontWeight.Medium),
    Font(R.font.keyboard_font, FontWeight.SemiBold),
    Font(R.font.keyboard_font, FontWeight.Bold)
)

val LocalKeyboardColorScheme = staticCompositionLocalOf {
    KeyboardColorScheme.DefaultDark
}

val LocalKeyboardFontFamily = staticCompositionLocalOf {
    KeyboardFontFamily
}

object KiebordTheme {
    val colorScheme: KeyboardColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalKeyboardColorScheme.current

    val fontFamily: FontFamily
        @Composable
        @ReadOnlyComposable
        get() = LocalKeyboardFontFamily.current
}

@Composable
fun KiebordTheme(
    colorScheme: KeyboardColorScheme = KeyboardColorScheme.DefaultDark,
    fontFamily: FontFamily = KeyboardFontFamily,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalKeyboardColorScheme provides colorScheme,
        LocalKeyboardFontFamily provides fontFamily,
        LocalTextStyle provides TextStyle(fontFamily = fontFamily),
        content = content
    )
}
