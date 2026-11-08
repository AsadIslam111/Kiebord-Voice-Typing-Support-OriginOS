package com.codepotro.borno.feature.themedesigner

import androidx.compose.runtime.Composable
import com.codepotro.borno.theme.designer.ThemeDesignerColors
import com.codepotro.borno.theme.designer.ThemeDesignerFeature
import com.codepotro.borno.theme.designer.ThemeDesignerPreviewConfig

/**
 * Runtime entry point discovered reflectively by [com.codepotro.borno.theme.designer.ThemeDesignerRegistry].
 *
 * Keeping this in `:feature:themedesigner` allows the entire Theme Studio to be excluded from a
 * future "lite" flavor simply by omitting the Gradle dependency on `:feature:themedesigner`.
 */
object ThemeDesignerFeatureImpl : ThemeDesignerFeature {

    @Composable
    override fun Screen(
        editingThemeId: String?,
        colors: ThemeDesignerColors,
        previewConfig: ThemeDesignerPreviewConfig,
        onBack: () -> Unit,
        onThemeSaved: (themeId: String) -> Unit
    ) {
        ThemeDesignerScreenContent(
            editingThemeId = editingThemeId,
            colors = colors,
            previewConfig = previewConfig,
            onBack = onBack,
            onThemeSaved = onThemeSaved
        )
    }
}

