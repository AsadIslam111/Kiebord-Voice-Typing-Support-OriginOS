package com.codepotro.borno.theme.designer

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Host-provided visual tokens so the Theme Designer module can match the host app's
 * settings UI without depending on the `:app` module.
 */
data class ThemeDesignerColors(
    val background: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val divider: Color,
    val inputBackground: Color,
    val inputBorder: Color,
    val isDark: Boolean
)

/**
 * Host-provided keyboard preview preferences (so the designer preview matches the user's keyboard).
 */
data class ThemeDesignerPreviewConfig(
    val showNumberRow: Boolean = false,
    val keyboardHeightScale: Float = 1.0f,
    val showKeyBorders: Boolean = true,
    val showKeyShadows: Boolean = true
)

/**
 * Optional feature contract for the Theme Designer (Theme Studio).
 *
 * The implementation lives in the `:feature:themedesigner` module. Hosts must never reference
 * that module directly; they resolve it through [ThemeDesignerRegistry]. This lets a "lite"
 * build exclude the module entirely (just drop the Gradle dependency) with no code changes.
 */
interface ThemeDesignerFeature {

    @Composable
    fun Screen(
        editingThemeId: String?,
        colors: ThemeDesignerColors,
        previewConfig: ThemeDesignerPreviewConfig,
        onBack: () -> Unit,
        onThemeSaved: (themeId: String) -> Unit
    )
}

/**
 * Resolves the optional [ThemeDesignerFeature] implementation at runtime.
 *
 * Returns `null` when the `:feature:themedesigner` module is not packaged into the build.
 */
object ThemeDesignerRegistry {

    const val IMPLEMENTATION_CLASS = "com.codepotro.borno.feature.themedesigner.ThemeDesignerFeatureImpl"

    @Volatile
    private var override: ThemeDesignerFeature? = null

    private val discovered: ThemeDesignerFeature? by lazy {
        runCatching {
            val clazz = Class.forName(IMPLEMENTATION_CLASS)
            // Kotlin `object` exposes its singleton through a static INSTANCE field.
            clazz.getField("INSTANCE").get(null) as? ThemeDesignerFeature
        }.getOrNull()
    }

    val feature: ThemeDesignerFeature?
        get() = override ?: discovered

    val isAvailable: Boolean
        get() = feature != null

    /** Explicit registration hook (e.g. for tests or a DI container). */
    fun register(implementation: ThemeDesignerFeature) {
        override = implementation
    }
}
