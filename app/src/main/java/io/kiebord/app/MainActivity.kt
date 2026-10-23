package io.kiebord.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.kiebord.app.ui.settings.AboutScreen
import io.kiebord.app.ui.settings.AppearanceThemesScreen
import io.kiebord.app.ui.settings.CorrectionsSuggestionsScreen
import io.kiebord.app.ui.settings.KeyboardLayoutsScreen
import io.kiebord.app.ui.settings.PreferencesScreen
import io.kiebord.app.ui.settings.SettingsMainScreen
import io.kiebord.app.ui.settings.SettingsScreen
import io.kiebord.app.ui.settings.SettingsUnifiedTopBar
import io.kiebord.data.prefs.KiebordPreferences
import io.kiebord.data.prefs.KiebordPreferencesRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var isImeEnabledState by mutableStateOf(false)
    private var isImeSelectedState by mutableStateOf(false)
    private lateinit var preferencesRepo: KiebordPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        preferencesRepo = KiebordPreferencesRepository.getInstance(this)
        updateImeStatus()

        setContent {
            val preferences by preferencesRepo.preferencesFlow.collectAsState(
                initial = KiebordPreferences()
            )

            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF131417),
                    surface = Color(0xFF1E1F22),
                    primary = Color(0xFF7AA2F7),
                    onPrimary = Color(0xFF0F141C),
                    onSurface = Color(0xFFE0E2E8)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                        updateImeStatus()
                    }

                    KiebordSettingsApp(
                        preferences = preferences,
                        preferencesRepo = preferencesRepo,
                        isImeEnabled = isImeEnabledState,
                        isImeSelected = isImeSelectedState,
                        onOpenSettings = {
                            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                        },
                        onSwitchIme = {
                            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showInputMethodPicker()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateImeStatus()
    }

    private fun updateImeStatus() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledMethods = imm.enabledInputMethodList
        isImeEnabledState = enabledMethods.any { it.packageName == packageName }

        val defaultIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        isImeSelectedState = defaultIme != null && defaultIme.contains(packageName)
    }
}

@Composable
fun KiebordSettingsApp(
    preferences: KiebordPreferences,
    preferencesRepo: KiebordPreferencesRepository,
    isImeEnabled: Boolean,
    isImeSelected: Boolean,
    onOpenSettings: () -> Unit,
    onSwitchIme: () -> Unit
) {
    var currentScreen by remember { mutableStateOf<SettingsScreen>(SettingsScreen.Main) }
    var backStack by remember { mutableStateOf<List<SettingsScreen>>(emptyList()) }
    var predictiveProgress by remember { mutableFloatStateOf(0f) }
    var isPredictiveBackActive by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val navigateTo: (SettingsScreen) -> Unit = { screen ->
        backStack = backStack + currentScreen
        currentScreen = screen
    }

    val navigateBack: () -> Unit = {
        if (backStack.isNotEmpty()) {
            currentScreen = backStack.last()
            backStack = backStack.dropLast(1)
        } else {
            currentScreen = SettingsScreen.Main
        }
    }

    PredictiveBackHandler(enabled = currentScreen != SettingsScreen.Main) { progress ->
        try {
            isPredictiveBackActive = true
            progress.collect { backEvent ->
                predictiveProgress = backEvent.progress
            }
            navigateBack()
        } catch (e: CancellationException) {
            // Cancelled
        } finally {
            predictiveProgress = 0f
            isPredictiveBackActive = false
        }
    }

    val predictiveModifier = if (isPredictiveBackActive) {
        Modifier.graphicsLayer {
            val scale = 1f - (predictiveProgress * 0.08f)
            scaleX = scale
            scaleY = scale
            translationX = predictiveProgress * 48.dp.toPx()
            alpha = 1f - (predictiveProgress * 0.2f)
            shape = RoundedCornerShape((predictiveProgress * 20f).dp)
            clip = true
        }
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Persistent Top Bar that stays anchored, only transitioning title/back-icon smoothly
        SettingsUnifiedTopBar(
            screen = currentScreen,
            onBack = navigateBack
        )

        // Inner Content changes smoothly without reloading the entire page
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .then(predictiveModifier)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    if (targetState == SettingsScreen.Main) {
                        (slideInHorizontally { -it / 3 } + fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally { it } + fadeOut(tween(180)))
                    } else {
                        (slideInHorizontally { it } + fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally { -it / 3 } + fadeOut(tween(180)))
                    }
                },
                label = "SettingsInnerContentTransition"
            ) { screen ->
                when (screen) {
                    is SettingsScreen.Main -> {
                        SettingsMainScreen(
                            preferences = preferences,
                            isImeEnabled = isImeEnabled,
                            isImeSelected = isImeSelected,
                            onOpenSettings = onOpenSettings,
                            onSwitchIme = onSwitchIme,
                            onNavigate = navigateTo
                        )
                    }

                    is SettingsScreen.AppearanceThemes -> {
                        AppearanceThemesScreen(
                            preferences = preferences,
                            onUpdateTheme = { theme ->
                                scope.launch { preferencesRepo.updateTheme(theme) }
                            },
                            onUpdateHeightScale = { scale ->
                                scope.launch { preferencesRepo.updateKeyboardHeightScale(scale) }
                            },
                            onUpdateShowKeyBorders = { enabled ->
                                scope.launch { preferencesRepo.updateShowKeyBorders(enabled) }
                            },
                            onBack = navigateBack
                        )
                    }

                    is SettingsScreen.KeyboardLayouts -> {
                        KeyboardLayoutsScreen(
                            preferences = preferences,
                            onUpdatePrimaryLayout = { layoutId ->
                                scope.launch { preferencesRepo.updatePrimaryLayoutId(layoutId) }
                            },
                            onToggleLayoutEnabled = { layoutId, enabled ->
                                scope.launch { preferencesRepo.toggleLayoutEnabled(layoutId, enabled) }
                            },
                            onBack = navigateBack
                        )
                    }

                    is SettingsScreen.Preferences -> {
                        PreferencesScreen(
                            preferences = preferences,
                            onUpdateVibrate = { enabled ->
                                scope.launch { preferencesRepo.updateVibrateOnKeypress(enabled) }
                            },
                            onUpdateVibrationStrength = { strength ->
                                scope.launch { preferencesRepo.updateVibrationStrength(strength) }
                            },
                            onUpdateSound = { enabled ->
                                scope.launch { preferencesRepo.updateSoundOnKeypress(enabled) }
                            },
                            onUpdateLongPressDelay = { delayMs ->
                                scope.launch { preferencesRepo.updateLongPressDelayMs(delayMs) }
                            },
                            onUpdateShowNumberRow = { enabled ->
                                scope.launch { preferencesRepo.updateShowNumberRow(enabled) }
                            },
                            onUpdateShowKeyPopups = { enabled ->
                                scope.launch { preferencesRepo.updateShowKeyPopups(enabled) }
                            },
                            onUpdateCursorTrackpad = { enabled ->
                                scope.launch { preferencesRepo.updateCursorTrackpadEnabled(enabled) }
                            },
                            onUpdateOneHandedMode = { mode ->
                                scope.launch { preferencesRepo.updateOneHandedMode(mode) }
                            },
                            onUpdateLongPressDelete = { enabled ->
                                scope.launch { preferencesRepo.updateLongPressDeleteEnabled(enabled) }
                            },
                            onUpdateSelectiveDeleteMode = { mode ->
                                scope.launch { preferencesRepo.updateSelectiveDeleteMode(mode) }
                            },
                            onBack = navigateBack
                        )
                    }

                    is SettingsScreen.CorrectionsSuggestions -> {
                        CorrectionsSuggestionsScreen(
                            preferences = preferences,
                            onUpdateSuggestionsEnabled = { enabled ->
                                scope.launch { preferencesRepo.updateSuggestionsEnabled(enabled) }
                            },
                            onUpdateNextWordPrediction = { enabled ->
                                scope.launch { preferencesRepo.updateNextWordPrediction(enabled) }
                            },
                            onUpdateBengaliSmartPhonetic = { enabled ->
                                scope.launch { preferencesRepo.updateBengaliSmartPhonetic(enabled) }
                            },
                            onUpdateGestureTyping = { enabled ->
                                scope.launch { preferencesRepo.updateGestureTypingEnabled(enabled) }
                            },
                            onUpdateShowGestureTrail = { enabled ->
                                scope.launch { preferencesRepo.updateShowGestureTrail(enabled) }
                            },
                            onUpdateAutoCapitalization = { enabled ->
                                scope.launch { preferencesRepo.updateAutoCapitalization(enabled) }
                            },
                            onUpdateDoubleSpacePeriod = { enabled ->
                                scope.launch { preferencesRepo.updateDoubleSpacePeriod(enabled) }
                            },
                            onBack = navigateBack
                        )
                    }

                    is SettingsScreen.About -> {
                        AboutScreen(
                            onResetDefaults = {
                                scope.launch { preferencesRepo.resetToDefaults() }
                            },
                            onBack = navigateBack
                        )
                    }
                }
            }
        }
    }
}
