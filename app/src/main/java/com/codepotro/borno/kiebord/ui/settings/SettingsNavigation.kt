package com.codepotro.borno.kiebord.ui.settings

/**
 * Defines the navigation destinations within Kiebord Settings.
 */
sealed class SettingsScreen {
    data object Main : SettingsScreen()
    data object AppearanceThemes : SettingsScreen()
    data class ThemeDesigner(val editingThemeId: String? = null) : SettingsScreen()
    data object KeyboardLayouts : SettingsScreen()
    data object LanguageExtensions : SettingsScreen()
    data object Preferences : SettingsScreen()
    data object SmartGestures : SettingsScreen()
    data object CorrectionsSuggestions : SettingsScreen()
    data object PersonalDictionary : SettingsScreen()
    data object Clipboard : SettingsScreen()
    data object ClipboardManager : SettingsScreen()
    data object VoiceTyping : SettingsScreen()
    data object BackupRestore : SettingsScreen()
    data object About : SettingsScreen()
    data object Onboarding : SettingsScreen()
}
