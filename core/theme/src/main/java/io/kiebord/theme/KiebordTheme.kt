package io.kiebord.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalKeyboardColorScheme = staticCompositionLocalOf {
    KeyboardColorScheme.DefaultDark
}

object KiebordTheme {
    val colorScheme: KeyboardColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalKeyboardColorScheme.current
}

@Composable
fun KiebordTheme(
    colorScheme: KeyboardColorScheme = KeyboardColorScheme.DefaultDark,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalKeyboardColorScheme provides colorScheme,
        content = content
    )
}
