package io.kiebord.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color

object DynamicKeyboardTheme {

    const val THEME_ADAPTIVE_ID = "dynamic_adaptive"
    const val THEME_ADAPTIVE_STROKED_ID = "dynamic_adaptive_stroked"

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun isSystemDarkTheme(context: Context): Boolean {
        val uiMode = context.resources.configuration.uiMode
        return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    fun isAdaptiveTheme(id: String): Boolean {
        return id == THEME_ADAPTIVE_ID || id == THEME_ADAPTIVE_STROKED_ID || id == "dynamic_adaptive_light_keys"
    }

    fun createColorScheme(
        context: Context,
        isDark: Boolean = isSystemDarkTheme(context),
        isStroked: Boolean = false
    ): KeyboardColorScheme {
        if (!isSupported()) {
            return if (isDark) KeyboardColorScheme.DefaultDark else KeyboardColorScheme.DefaultLight
        }

        val m3Scheme = if (isDark) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }

        val border = if (isStroked) {
            val alpha = if (isDark) 0.35f else 0.45f
            m3Scheme.outlineVariant.copy(alpha = alpha)
        } else {
            Color.Transparent
        }

        val containerBg = m3Scheme.surfaceContainer
        val keyBg = if (isDark) m3Scheme.surfaceContainerHighest else m3Scheme.surfaceContainerLowest
        val keyFg = m3Scheme.onSurface
        val modBg = if (isDark) m3Scheme.surfaceBright else m3Scheme.surfaceContainerHighest
        val modFg = if (isDark) m3Scheme.onSurface else m3Scheme.onSurfaceVariant
        val actionBg = if (isDark) m3Scheme.onPrimaryContainer else m3Scheme.primaryContainer
        val actionFg = if (isDark) {
            if (ThemeColorUtils.isColorDark(actionBg)) m3Scheme.onSurface else m3Scheme.onPrimary
        } else {
            m3Scheme.onPrimaryContainer
        }
        val accent = if (isDark) actionBg else m3Scheme.primary
        val pressedBg = if (isDark) m3Scheme.primary else m3Scheme.surfaceContainerHigh
        val popBg = m3Scheme.surfaceContainerHighest
        val shadow = if (isDark) Color(0x33000000) else Color(0x1F000000)

        return KeyboardColorScheme(
            containerBackground = containerBg,
            keyBackground = keyBg,
            keyForeground = keyFg,
            keyBorder = border,
            keyShadow = shadow,
            actionKeyBackground = actionBg,
            actionKeyForeground = actionFg,
            modifierKeyBackground = modBg,
            modifierKeyForeground = modFg,
            shiftActiveColor = accent,
            accentColor = accent,
            accentForeground = actionFg,
            moreKeysBackground = popBg,
            guidelineColor = accent,
            suggestionBackground = containerBg,
            suggestionForeground = keyFg,
            keyPressedBackground = pressedBg,
            keyPressedForeground = actionFg,
            flatKeyForeground = keyFg,
            activeOptionHighlightColor = accent,
            isAdaptive = true
        )
    }
}
