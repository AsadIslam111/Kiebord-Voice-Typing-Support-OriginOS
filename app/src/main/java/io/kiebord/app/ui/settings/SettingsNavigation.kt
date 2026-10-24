package io.kiebord.app.ui.settings

/**
 * Defines the navigation destinations within Kiebord Settings.
 */
sealed class SettingsScreen {
    data object Main : SettingsScreen()
    data object AppearanceThemes : SettingsScreen()
    data object KeyboardLayouts : SettingsScreen()
    data object Preferences : SettingsScreen()
    data object CorrectionsSuggestions : SettingsScreen()
    data object About : SettingsScreen()
    data object Onboarding : SettingsScreen()
}
