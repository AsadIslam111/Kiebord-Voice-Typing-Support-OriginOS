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

val KiebordTypography = androidx.compose.material3.Typography().let { defaultTypography ->
    androidx.compose.material3.Typography(
        displayLarge = defaultTypography.displayLarge.copy(fontFamily = KeyboardFontFamily),
        displayMedium = defaultTypography.displayMedium.copy(fontFamily = KeyboardFontFamily),
        displaySmall = defaultTypography.displaySmall.copy(fontFamily = KeyboardFontFamily),
        headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = KeyboardFontFamily),
        headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = KeyboardFontFamily),
        headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = KeyboardFontFamily),
        titleLarge = defaultTypography.titleLarge.copy(fontFamily = KeyboardFontFamily),
        titleMedium = defaultTypography.titleMedium.copy(fontFamily = KeyboardFontFamily),
        titleSmall = defaultTypography.titleSmall.copy(fontFamily = KeyboardFontFamily),
        bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = KeyboardFontFamily),
        bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = KeyboardFontFamily),
        bodySmall = defaultTypography.bodySmall.copy(fontFamily = KeyboardFontFamily),
        labelLarge = defaultTypography.labelLarge.copy(fontFamily = KeyboardFontFamily),
        labelMedium = defaultTypography.labelMedium.copy(fontFamily = KeyboardFontFamily),
        labelSmall = defaultTypography.labelSmall.copy(fontFamily = KeyboardFontFamily)
    )
}

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
    fontFamily: FontFamily? = null,
    content: @Composable () -> Unit
) {
    val resolvedFontFamily = fontFamily ?: colorScheme.effectiveFontFamily ?: KeyboardFontFamily
    CompositionLocalProvider(
        LocalKeyboardColorScheme provides colorScheme,
        LocalKeyboardFontFamily provides resolvedFontFamily,
        LocalTextStyle provides TextStyle(fontFamily = resolvedFontFamily),
        content = content
    )
}
