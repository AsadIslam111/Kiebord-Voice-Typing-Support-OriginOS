package io.kiebord.feature.ime

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.kiebord.common.model.KeyDef
import io.kiebord.common.model.KeyType
import io.kiebord.common.model.KeyboardLayoutDef
import io.kiebord.common.model.ShiftState
import io.kiebord.data.dictionary.UserDictionaryRepository
import io.kiebord.data.layout.PredefinedLayouts
import io.kiebord.feature.ime.clipboard.SandboxedClipboardManager
import io.kiebord.feature.ime.gesture.CrossLanguageGestureDecoder
import io.kiebord.feature.ime.gesture.KeyCentroid
import io.kiebord.feature.ime.lifecycle.KeyboardViewLifecycleOwner
import io.kiebord.feature.ime.translit.BornoTransliterationManager
import io.kiebord.feature.ime.translit.TransliterationManager
import androidx.compose.ui.unit.dp
import io.kiebord.data.prefs.KiebordPreferences
import io.kiebord.data.prefs.KiebordPreferencesRepository
import io.kiebord.feature.ime.translit.TransliterationManagerFactory
import io.kiebord.feature.ime.ui.KeyboardRootView
import io.kiebord.feature.ime.ui.suggestion.SuggestionCandidate
import io.kiebord.feature.ime.ui.textedit.TextAction
import io.kiebord.feature.ime.ui.toolbar.ToolbarAction
import io.kiebord.theme.KeyboardColorScheme
import io.kiebord.theme.KiebordTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class KiebordService : InputMethodService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var preferencesRepo: KiebordPreferencesRepository
    private var preferencesState by mutableStateOf(KiebordPreferences())

    private val lifecycleOwner = KeyboardViewLifecycleOwner()
    private var transliterationManager: TransliterationManager =
        TransliterationManagerFactory.create("bn_borno") { currentInputConnection }
    private val composingBuffer = StringBuilder()
    private val gestureDecoder = CrossLanguageGestureDecoder()
    private var lastGestureCommittedWord: String = ""

    private fun isTransliterationLayout(): Boolean {
        return TransliterationManagerFactory.isTransliterationLayout(activeLayout.id)
    }

    private fun getTransliterationManager(): TransliterationManager {
        if (transliterationManager.layoutId != activeLayout.id) {
            transliterationManager = TransliterationManagerFactory.create(activeLayout.id) { currentInputConnection }
        }
        return transliterationManager
    }

    // Configurable state
    private var primaryAlphaLayout: KeyboardLayoutDef = PredefinedLayouts.EnglishQwerty
    private var activeLayout by mutableStateOf(PredefinedLayouts.EnglishQwerty)
    private var shiftState by mutableStateOf(ShiftState.UNSHIFTED)
    private var isSymbolsMode by mutableStateOf(false)
    private var colorScheme by mutableStateOf(KeyboardColorScheme.DefaultDark)
    private var navBarInsetBottomPx by mutableIntStateOf(0)
    private var activeSuggestions by mutableStateOf<List<SuggestionCandidate>>(emptyList())
    private lateinit var clipboardManager: SandboxedClipboardManager
    private lateinit var userDictionaryRepo: UserDictionaryRepository

    override fun onCreate() {
        super.onCreate()
        preferencesRepo = KiebordPreferencesRepository.getInstance(this)
        clipboardManager = SandboxedClipboardManager(this)
        userDictionaryRepo = UserDictionaryRepository.getInstance(this)
        lifecycleOwner.onCreate()
        NativeEngine.ensureInitialized(this, "en")
        NativeEngine.ensureInitialized(this, "bn")

        serviceScope.launch {
            preferencesRepo.preferencesFlow.collect { prefs ->
                preferencesState = prefs
                colorScheme = when (prefs.theme) {
                    KiebordPreferences.THEME_AMOLED_BLACK -> KeyboardColorScheme.AmoledBlack
                    KiebordPreferences.THEME_TOKYO_NIGHT -> KeyboardColorScheme.TokyoNight
                    KiebordPreferences.THEME_NORD -> KeyboardColorScheme.Nord
                    KiebordPreferences.THEME_DEFAULT_LIGHT -> KeyboardColorScheme.DefaultLight
                    else -> KeyboardColorScheme.DefaultDark
                }
                val enabledIds = prefs.enabledLayoutIds.toList().filter { id ->
                    id == KiebordPreferences.LAYOUT_EN_QWERTY ||
                    id == KiebordPreferences.LAYOUT_BN_BORNO ||
                    id == KiebordPreferences.LAYOUT_BN_PROBHAT
                }.ifEmpty { listOf(KiebordPreferences.LAYOUT_EN_QWERTY) }

                val targetPrimaryId = if (enabledIds.contains(prefs.primaryLayoutId)) {
                    prefs.primaryLayoutId
                } else {
                    enabledIds.first()
                }

                val targetPrimary = when (targetPrimaryId) {
                    KiebordPreferences.LAYOUT_BN_BORNO -> PredefinedLayouts.BengaliBorno
                    KiebordPreferences.LAYOUT_BN_PROBHAT -> PredefinedLayouts.BengaliProbhat
                    else -> PredefinedLayouts.EnglishQwerty
                }
                if (primaryAlphaLayout.id != targetPrimary.id || !enabledIds.contains(primaryAlphaLayout.id)) {
                    primaryAlphaLayout = targetPrimary
                    if (!isSymbolsMode) {
                        activeLayout = targetPrimary
                    }
                }
            }
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onConfigureWindow(win: Window, isFullscreen: Boolean, isCandidatesOnly: Boolean) {
        super.onConfigureWindow(win, false, isCandidatesOnly)
        lifecycleOwner.attachToView(win.decorView)
    }

    override fun onCreateInputView(): View {
        window?.window?.decorView?.let { decorView ->
            lifecycleOwner.attachToView(decorView)
            ViewCompat.getRootWindowInsets(decorView)?.let { insets ->
                val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                val gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
                val mandatory = insets.getInsets(WindowInsetsCompat.Type.mandatorySystemGestures())
                val bottom = maxOf(nav.bottom, gestures.bottom, mandatory.bottom)
                if (bottom > 0) {
                    navBarInsetBottomPx = bottom
                }
            }
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(lifecycleOwner))
            lifecycleOwner.attachToView(this)
        }

        ViewCompat.setOnApplyWindowInsetsListener(composeView) { _, insets ->
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
            val mandatory = insets.getInsets(WindowInsetsCompat.Type.mandatorySystemGestures())
            val bottom = maxOf(nav.bottom, gestures.bottom, mandatory.bottom)
            navBarInsetBottomPx = bottom
            insets
        }

        composeView.setContent {
            val density = LocalDensity.current
            val navBarBottomDp = with(density) { navBarInsetBottomPx.toDp() }

            val defaultPinnedActions = listOf(ToolbarAction.TEXT_EDIT, ToolbarAction.CLIPBOARD, ToolbarAction.THEME_TOGGLE, ToolbarAction.RESIZER)
            val pinnedToolbarActions = preferencesState.pinnedTools.mapNotNull { name ->
                try { ToolbarAction.valueOf(name) } catch (_: Exception) { null }
            }.distinct().ifEmpty { defaultPinnedActions }
            val pinnedSet = pinnedToolbarActions.toSet()
            val drawerToolbarActions = (preferencesState.drawerTools.mapNotNull { name ->
                try { ToolbarAction.valueOf(name) } catch (_: Exception) { null }
            }.filter { it !in pinnedSet } + ToolbarAction.values().filter { it !in pinnedSet }).distinct()

            KiebordTheme(colorScheme = colorScheme) {
                KeyboardRootView(
                    layout = activeLayout,
                    shiftState = shiftState,
                    navBarBottomInset = navBarBottomDp,
                    onKeyPress = { performKeyFeedback() },
                    onKeyTap = ::handleKeyTap,
                    onKeyLongPress = ::handleKeyLongPress,
                    onTextAction = ::handleTextAction,
                    suggestions = activeSuggestions,
                    onCandidateSelect = ::handleCandidateSelect,
                    clipboardManager = clipboardManager,
                    onPasteClip = { clipText ->
                        val ic = currentInputConnection
                        finishComposing()
                        lastGestureCommittedWord = ""
                        ic?.commitText(clipText, 1)
                        updateSuggestions()
                    },
                    onGestureCommit = ::handleGestureCommit,
                    initialRowHeight = (54 * preferencesState.keyboardHeightScale).dp,
                    defaultShowNumberRow = preferencesState.showNumberRow,
                    onNumberRowToggle = { enabled ->
                        serviceScope.launch { preferencesRepo.updateShowNumberRow(enabled) }
                    },
                    showKeyBorders = preferencesState.showKeyBorders,
                    defaultShowSecondaryKeys = preferencesState.showSecondaryKeys,
                    onSecondaryKeysToggle = { enabled ->
                        serviceScope.launch { preferencesRepo.updateShowSecondaryKeys(enabled) }
                    },
                    initialPinnedTools = pinnedToolbarActions,
                    initialDrawerTools = drawerToolbarActions,
                    onToolbarToolsChange = { newPinned, newDrawer ->
                        serviceScope.launch {
                            preferencesRepo.updateToolbarTools(
                                newPinned.map { it.name },
                                newDrawer.map { it.name }
                            )
                        }
                    },
                    onHeightScaleChange = { scale ->
                        serviceScope.launch {
                            preferencesRepo.updateKeyboardHeightScale(scale)
                        }
                    },
                    onDeleteSwipeMove = ::handleDeleteSwipeMove,
                    onDeleteSwipeEnd = ::handleDeleteSwipeEnd,
                    onDeleteSwipeCancel = ::handleDeleteSwipeCancel,
                    enabledLayoutIds = preferencesState.enabledLayoutIds.toList().filter { id ->
                        id == KiebordPreferences.LAYOUT_EN_QWERTY ||
                        id == KiebordPreferences.LAYOUT_BN_BORNO ||
                        id == KiebordPreferences.LAYOUT_BN_PROBHAT
                    }.ifEmpty { listOf(KiebordPreferences.LAYOUT_EN_QWERTY) },
                    onSwitchToLayout = ::switchToLayout,
                    onOpenLanguagesSettings = ::openLanguagesSettings,
                    onOpenInputMethodPicker = ::openInputMethodPicker,
                    onThemeToggle = {
                        val nextThemeId = when (preferencesState.theme) {
                            KiebordPreferences.THEME_DEFAULT_DARK -> KiebordPreferences.THEME_AMOLED_BLACK
                            KiebordPreferences.THEME_AMOLED_BLACK -> KiebordPreferences.THEME_TOKYO_NIGHT
                            KiebordPreferences.THEME_TOKYO_NIGHT -> KiebordPreferences.THEME_NORD
                            KiebordPreferences.THEME_NORD -> KiebordPreferences.THEME_DEFAULT_LIGHT
                            else -> KiebordPreferences.THEME_DEFAULT_DARK
                        }
                        serviceScope.launch {
                            preferencesRepo.updateTheme(nextThemeId)
                        }
                    }
                )
            }
        }
        return composeView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        window?.window?.decorView?.let { decorView ->
            lifecycleOwner.attachToView(decorView)
            ViewCompat.getRootWindowInsets(decorView)?.let { insets ->
                val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                val gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
                val mandatory = insets.getInsets(WindowInsetsCompat.Type.mandatorySystemGestures())
                val bottom = maxOf(nav.bottom, gestures.bottom, mandatory.bottom)
                if (bottom > 0) {
                    navBarInsetBottomPx = bottom
                }
            }
        }
        lifecycleOwner.onStart()
        shiftState = ShiftState.UNSHIFTED
        isSymbolsMode = false
        activeLayout = primaryAlphaLayout
        getTransliterationManager().reset()
        composingBuffer.clear()
        lastGestureCommittedWord = ""
        updateSuggestions()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        finishComposing()
        activeSuggestions = emptyList()
        lifecycleOwner.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        lifecycleOwner.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        userDictionaryRepo.trimMemory(level)
        clipboardManager?.purgeOldClips()
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            System.gc()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        userDictionaryRepo.trimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
        clipboardManager?.purgeOldClips()
        System.gc()
    }

    fun switchToLayout(layoutId: String) {
        finishComposing()
        primaryAlphaLayout = PredefinedLayouts.fromId(layoutId)
        if (!isSymbolsMode) {
            activeLayout = primaryAlphaLayout
        }
        updateSuggestions()
    }

    fun openLanguagesSettings() {
        try {
            val intent = Intent(this, Class.forName("io.kiebord.app.MainActivity")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "layouts")
            }
            startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openInputMethodPicker() {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        } catch (_: Exception) {}
    }

    private fun finishComposing() {
        val ic = currentInputConnection
        val translitMgr = getTransliterationManager()
        if (translitMgr.isComposing) {
            ic?.commitText(translitMgr.currentTransliteration, 1)
            translitMgr.reset()
        }
        if (composingBuffer.isNotEmpty()) {
            ic?.commitText(composingBuffer.toString(), 1)
            composingBuffer.clear()
        }
        ic?.finishComposingText()
    }

    private fun performKeyFeedback() {
        if (!preferencesState.vibrateOnKeypress) return
        try {
            if (preferencesState.vibrationType == KiebordPreferences.VIBRATION_TYPE_HAPTIC) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    }
                    vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    window?.window?.decorView?.performHapticFeedback(
                        HapticFeedbackConstants.KEYBOARD_TAP,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    )
                }
            } else {
                // Vibration mode: duration and amplitude scaled by vibrationStrength
                val durationMs = (10L + (preferencesState.vibrationStrength * 30L / 100L)).coerceIn(5L, 60L)
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val amplitude = (preferencesState.vibrationStrength * 255 / 100).coerceIn(1, 255)
                    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {
            try {
                window?.window?.decorView?.performHapticFeedback(
                    HapticFeedbackConstants.KEYBOARD_TAP,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            } catch (_: Exception) {}
        }
    }

    private fun handleKeyTap(key: KeyDef) {
        val ic = currentInputConnection ?: return

        if (key.type != KeyType.DELETE) {
            lastGestureCommittedWord = ""
        }

        when (key.type) {
            KeyType.CHARACTER -> {
                val text = key.displayLabel(shiftState)

                when {
                    isTransliterationLayout() -> {
                        if (isTransliterableChar(text)) {
                            getTransliterationManager().handleCharacter(text)
                        } else {
                            finishComposing()
                            ic.commitText(text, 1)
                        }
                    }
                    // English Qwerty mode
                    activeLayout.id == "en_qwerty" -> {
                        if (isEnglishLetter(text)) {
                            composingBuffer.append(text)
                            ic.setComposingText(composingBuffer.toString(), 1)
                        } else {
                            finishComposing()
                            ic.commitText(text, 1)
                        }
                    }
                    // Bengali Probhat layout
                    activeLayout.id == "bn_probhat" -> {
                        if (isBengaliChar(text)) {
                            composingBuffer.append(text)
                            ic.setComposingText(composingBuffer.toString(), 1)
                        } else {
                            finishComposing()
                            ic.commitText(text, 1)
                        }
                    }
                    // Other layouts (Symbols, etc.)
                    else -> {
                        finishComposing()
                        ic.commitText(text, 1)
                    }
                }

                updateSuggestions()

                // If in one-shot shift mode, revert to unshifted after one character
                if (shiftState == ShiftState.ONE_SHOT) {
                    shiftState = ShiftState.UNSHIFTED
                }
            }
            KeyType.SHIFT -> {
                shiftState = when (shiftState) {
                    ShiftState.UNSHIFTED -> ShiftState.ONE_SHOT
                    ShiftState.ONE_SHOT -> ShiftState.CAPS_LOCK
                    ShiftState.CAPS_LOCK -> ShiftState.UNSHIFTED
                }
            }
            KeyType.DELETE -> {
                val selectedText = ic.getSelectedText(0)
                if (!selectedText.isNullOrEmpty()) {
                    ic.commitText("", 1)
                    updateSuggestions()
                    return
                }
                if (lastGestureCommittedWord.isNotEmpty()) {
                    ic.deleteSurroundingText(lastGestureCommittedWord.length, 0)
                    lastGestureCommittedWord = ""
                    updateSuggestions()
                    return
                }
                if (isTransliterationLayout() && getTransliterationManager().handleDelete()) {
                    updateSuggestions()
                    return
                }
                // If composing in English or Bengali Probhat
                if (composingBuffer.isNotEmpty()) {
                    composingBuffer.deleteCharAt(composingBuffer.length - 1)
                    if (composingBuffer.isEmpty()) {
                        ic.setComposingText("", 1)
                        ic.finishComposingText()
                    } else {
                        ic.setComposingText(composingBuffer.toString(), 1)
                    }
                    updateSuggestions()
                    return
                }

                ic.deleteSurroundingText(1, 0)
                updateSuggestions()
            }
            KeyType.ACTION -> {
                finishComposing()
                val currentAction = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                if (currentAction != null && currentAction != EditorInfo.IME_ACTION_NONE && currentAction != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(currentAction)
                } else {
                    sendKeyChar('\n')
                }
                updateSuggestions()
            }
            KeyType.SPACE -> {
                val translitMgr = getTransliterationManager()
                if (translitMgr.isComposing) {
                    val prevWord = getPrecedingWord(excludeComposing = true)
                    val committed = translitMgr.currentTransliteration
                    ic.commitText(committed + " ", 1)
                    if (preferencesState.learnedWordsEnabled && committed.isNotBlank()) {
                        userDictionaryRepo.recordWord(committed.trim(), prevWord)
                    }
                    translitMgr.reset()
                } else if (composingBuffer.isNotEmpty()) {
                    val prevWord = getPrecedingWord(excludeComposing = true)
                    val committed = composingBuffer.toString()
                    ic.commitText(committed + " ", 1)
                    if (preferencesState.learnedWordsEnabled && committed.isNotBlank()) {
                        userDictionaryRepo.recordWord(committed.trim(), prevWord)
                    }
                    composingBuffer.clear()
                } else {
                    ic.commitText(" ", 1)
                }
                updateSuggestions()
            }
            KeyType.SYMBOLS -> {
                finishComposing()
                when (key.label) {
                    "?123" -> {
                        if (!isSymbolsMode) {
                            primaryAlphaLayout = activeLayout
                            isSymbolsMode = true
                        }
                        activeLayout = PredefinedLayouts.Symbols
                    }
                    "=\\<", "=<" -> {
                        isSymbolsMode = true
                        activeLayout = PredefinedLayouts.SymbolsMore
                    }
                    "ABC" -> {
                        isSymbolsMode = false
                        activeLayout = primaryAlphaLayout
                    }
                    else -> {
                        if (isSymbolsMode) {
                            isSymbolsMode = false
                            activeLayout = primaryAlphaLayout
                        } else {
                            primaryAlphaLayout = activeLayout
                            isSymbolsMode = true
                            activeLayout = PredefinedLayouts.Symbols
                        }
                    }
                }
                updateSuggestions()
            }
            KeyType.SWITCH_LANGUAGE -> {
                finishComposing()
                val enabledIds = preferencesState.enabledLayoutIds.toList().filter { id ->
                    id == KiebordPreferences.LAYOUT_EN_QWERTY ||
                    id == KiebordPreferences.LAYOUT_BN_BORNO ||
                    id == KiebordPreferences.LAYOUT_BN_PROBHAT
                }.ifEmpty { listOf(KiebordPreferences.LAYOUT_EN_QWERTY) }

                if (enabledIds.size > 1) {
                    val currentIndex = enabledIds.indexOf(primaryAlphaLayout.id)
                    val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % enabledIds.size else 0
                    val nextId = enabledIds[nextIndex]
                    primaryAlphaLayout = when (nextId) {
                        KiebordPreferences.LAYOUT_BN_BORNO -> PredefinedLayouts.BengaliBorno
                        KiebordPreferences.LAYOUT_BN_PROBHAT -> PredefinedLayouts.BengaliProbhat
                        else -> PredefinedLayouts.EnglishQwerty
                    }
                    if (!isSymbolsMode) {
                        activeLayout = primaryAlphaLayout
                    }
                    updateSuggestions()
                }
            }
            KeyType.MORE_KEYS -> {
                // Secondary popups
            }
        }
    }

    private fun handleKeyLongPress(key: KeyDef) {
        if (key.type == KeyType.SWITCH_LANGUAGE) {
            return
        }
        if (key.type == KeyType.SPACE) {
            return
        }
        if (key.type == KeyType.DELETE) {
            handleLongPressDelete()
            return
        }
        val ic = currentInputConnection ?: return
        finishComposing()
        if (!key.flickUp.isNullOrEmpty()) {
            ic.commitText(key.flickUp!!, 1)
        } else if (key.moreKeys.isNotEmpty()) {
            ic.commitText(key.moreKeys.first(), 1)
        }
        updateSuggestions()
    }

    private var isSwipeDeleting = false
    private var swipeDeleteInitialCursor = -1
    private var swipeDeleteInitialText = ""

    private fun handleDeleteSwipeMove(deltaX: Float) {
        val ic = currentInputConnection ?: return
        if (!isSwipeDeleting) {
            isSwipeDeleting = true
            finishComposing()
            val req = ExtractedTextRequest().apply { token = 0; flags = 0 }
            val ext = ic.getExtractedText(req, 0)
            swipeDeleteInitialCursor = ext?.let { it.startOffset + it.selectionEnd } ?: -1
            swipeDeleteInitialText = ic.getTextBeforeCursor(1024, 0)?.toString() ?: ""
        }

        val absDistance = (-deltaX).coerceAtLeast(0f)
        val density = resources.displayMetrics.density
        val textLen = swipeDeleteInitialText.length

        val charsToSelect = if (preferencesState.selectiveDeleteMode == KiebordPreferences.DELETE_MODE_WORD) {
            val stepPx = 28f * density
            val wordsToSelect = (absDistance / stepPx).toInt().coerceAtLeast(0)
            var idx = textLen
            var wordsFound = 0
            while (idx > 0 && wordsFound < wordsToSelect) {
                while (idx > 0 && swipeDeleteInitialText[idx - 1].isWhitespace()) {
                    idx--
                }
                while (idx > 0 && !swipeDeleteInitialText[idx - 1].isWhitespace()) {
                    idx--
                }
                wordsFound++
            }
            textLen - idx
        } else {
            val stepPx = 11f * density
            (absDistance / stepPx).toInt().coerceIn(0, textLen)
        }

        if (swipeDeleteInitialCursor != -1) {
            val start = (swipeDeleteInitialCursor - charsToSelect).coerceAtLeast(0)
            ic.setSelection(start, swipeDeleteInitialCursor)
        } else {
            val start = (textLen - charsToSelect).coerceAtLeast(0)
            ic.setSelection(start, textLen)
        }
    }

    private fun handleDeleteSwipeEnd() {
        val ic = currentInputConnection ?: return
        if (isSwipeDeleting) {
            isSwipeDeleting = false
            val selectedText = ic.getSelectedText(0)
            if (!selectedText.isNullOrEmpty()) {
                ic.commitText("", 1)
            }
            updateSuggestions()
        }
    }

    private fun handleDeleteSwipeCancel() {
        val ic = currentInputConnection ?: return
        if (isSwipeDeleting) {
            isSwipeDeleting = false
            if (swipeDeleteInitialCursor != -1) {
                ic.setSelection(swipeDeleteInitialCursor, swipeDeleteInitialCursor)
            } else {
                ic.setSelection(swipeDeleteInitialText.length, swipeDeleteInitialText.length)
            }
        }
    }

    private fun handleLongPressDelete() {
        if (!preferencesState.longPressDeleteEnabled) {
            handleKeyTap(KeyDef(type = KeyType.DELETE))
            return
        }
        val ic = currentInputConnection ?: return

        // 1. If text is highlighted / selected, delete the whole selection first
        val selectedText = ic.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) {
            ic.commitText("", 1)
            updateSuggestions()
            return
        }

        // 2. Gesture-committed word
        if (lastGestureCommittedWord.isNotEmpty()) {
            ic.deleteSurroundingText(lastGestureCommittedWord.length, 0)
            lastGestureCommittedWord = ""
            updateSuggestions()
            return
        }

        // 3. Active transliteration composing
        if (isTransliterationLayout()) {
            val translitMgr = getTransliterationManager()
            if (translitMgr.isComposing) {
                translitMgr.handleDelete()
                updateSuggestions()
                return
            }
        }

        // 4. Standard composing buffer
        if (composingBuffer.isNotEmpty()) {
            composingBuffer.deleteCharAt(composingBuffer.length - 1)
            if (composingBuffer.isEmpty()) {
                ic.setComposingText("", 1)
                ic.finishComposingText()
            } else {
                ic.setComposingText(composingBuffer.toString(), 1)
            }
            updateSuggestions()
            return
        }

        // 5. Normal continuous deletion like Borno Keyboard
        ic.deleteSurroundingText(1, 0)
        updateSuggestions()
    }

    private fun isTransliterableChar(text: String): Boolean {
        if (text.length != 1) return false
        val c = text[0]
        return (c in 'a'..'z') || (c in 'A'..'Z')
    }

    private fun isEnglishLetter(text: String): Boolean {
        if (text.length != 1) return false
        val c = text[0]
        return (c in 'a'..'z') || (c in 'A'..'Z') || c == '\''
    }

    private fun isBengaliChar(text: String): Boolean {
        if (text.isEmpty()) return false
        val cp = text.codePointAt(0)
        return cp in 0x0980..0x09FF
    }

    private fun handleTextAction(action: TextAction, isSelecting: Boolean) {
        val ic = currentInputConnection ?: return
        finishComposing()

        when (action) {
            TextAction.MOVE_UP -> sendNavKeyEvent(ic, KeyEvent.KEYCODE_DPAD_UP, isSelecting)
            TextAction.MOVE_DOWN -> sendNavKeyEvent(ic, KeyEvent.KEYCODE_DPAD_DOWN, isSelecting)
            TextAction.MOVE_LEFT -> sendNavKeyEvent(ic, KeyEvent.KEYCODE_DPAD_LEFT, isSelecting)
            TextAction.MOVE_RIGHT -> sendNavKeyEvent(ic, KeyEvent.KEYCODE_DPAD_RIGHT, isSelecting)
            TextAction.WORD_LEFT -> sendNavKeyEvent(ic, KeyEvent.KEYCODE_DPAD_LEFT, isSelecting, isWord = true)
            TextAction.WORD_RIGHT -> sendNavKeyEvent(ic, KeyEvent.KEYCODE_DPAD_RIGHT, isSelecting, isWord = true)
            TextAction.LINE_START -> sendNavKeyEvent(ic, KeyEvent.KEYCODE_MOVE_HOME, isSelecting)
            TextAction.LINE_END -> sendNavKeyEvent(ic, KeyEvent.KEYCODE_MOVE_END, isSelecting)
            TextAction.SELECT_ALL -> ic.performContextMenuAction(android.R.id.selectAll)
            TextAction.CUT -> ic.performContextMenuAction(android.R.id.cut)
            TextAction.COPY -> ic.performContextMenuAction(android.R.id.copy)
            TextAction.PASTE -> ic.performContextMenuAction(android.R.id.paste)
            TextAction.DELETE_BACKWARD -> {
                val selected = ic.getSelectedText(0)
                if (!selected.isNullOrEmpty()) {
                    ic.commitText("", 1)
                } else {
                    ic.deleteSurroundingText(1, 0)
                }
            }
            TextAction.DELETE_FORWARD -> {
                val selected = ic.getSelectedText(0)
                if (!selected.isNullOrEmpty()) {
                    ic.commitText("", 1)
                } else {
                    ic.deleteSurroundingText(0, 1)
                }
            }
            TextAction.ENTER -> {
                val currentAction = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                if (currentAction != null && currentAction != EditorInfo.IME_ACTION_NONE && currentAction != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(currentAction)
                } else {
                    sendKeyChar('\n')
                }
            }
        }
        updateSuggestions()
    }

    private fun getPrecedingWord(excludeComposing: Boolean = false): String {
        val ic = currentInputConnection ?: return ""
        val text = ic.getTextBeforeCursor(100, 0)?.toString() ?: return ""
        if (text.isEmpty()) return ""

        val translitMgr = getTransliterationManager()
        val isCurrentlyComposing = translitMgr.isComposing || composingBuffer.isNotEmpty()

        val textToProcess = if (excludeComposing && isCurrentlyComposing) {
            val composing = when {
                translitMgr.isComposing -> translitMgr.currentTransliteration
                composingBuffer.isNotEmpty() -> composingBuffer.toString()
                else -> ""
            }
            val raw = if (translitMgr.isComposing) translitMgr.currentRawInput else ""

            when {
                composing.isNotEmpty() && text.endsWith(composing) -> text.dropLast(composing.length)
                composing.isNotEmpty() && text.trimEnd().endsWith(composing) -> text.trimEnd().dropLast(composing.length)
                raw.isNotEmpty() && text.endsWith(raw) -> text.dropLast(raw.length)
                raw.isNotEmpty() && text.trimEnd().endsWith(raw) -> text.trimEnd().dropLast(raw.length)
                else -> {
                    val tokens = text.trimEnd().split(Regex("[\\s,.;:!?\"'()\\[\\]{}]+")).filter { it.isNotEmpty() }
                    return if (tokens.size >= 2) tokens[tokens.size - 2] else ""
                }
            }
        } else {
            text
        }

        val tokens = textToProcess.trimEnd().split(Regex("[\\s,.;:!?\"'()\\[\\]{}]+")).filter { it.isNotEmpty() }
        return tokens.lastOrNull() ?: ""
    }

    private fun handleCandidateSelect(candidate: SuggestionCandidate) {
        performKeyFeedback()
        val ic = currentInputConnection ?: return
        val prevWord = getPrecedingWord(excludeComposing = true)
        val candidateText = candidate.text.trim()
        if (preferencesState.learnedWordsEnabled && candidateText.isNotEmpty()) {
            userDictionaryRepo.recordWord(candidateText, prevWord)
        }
        if (lastGestureCommittedWord.isNotEmpty()) {
            ic.deleteSurroundingText(lastGestureCommittedWord.length, 0)
            lastGestureCommittedWord = ""
            ic.commitText(candidate.text + " ", 1)
            updateSuggestions()
            return
        }
        val translitMgr = getTransliterationManager()
        if (translitMgr.isComposing) {
            translitMgr.commitCustom(candidate.text + " ")
        } else if (composingBuffer.isNotEmpty()) {
            ic.commitText(candidate.text + " ", 1)
            composingBuffer.clear()
        } else {
            ic.commitText(candidate.text + " ", 1)
        }
        updateSuggestions()
    }

    private fun handleGestureCommit(
        trail: List<androidx.compose.ui.geometry.Offset>,
        centroids: List<KeyCentroid>,
        keyWidthPx: Float
    ) {
        if (trail.size < 2 || centroids.isEmpty()) return

        val lang = activeLayout.id
        val learnedGestureWords = if (preferencesState.learnedWordsEnabled) {
            userDictionaryRepo.getLearnedGestureWords(10)
        } else emptyList()

        val candidates = gestureDecoder.decodeGesture(trail, centroids, keyWidthPx, lang, learnedGestureWords)
        if (candidates.isNotEmpty()) {
            val primary = candidates.first()
            val textToCommit = primary.text + " "
            val ic = currentInputConnection
            val prevWord = getPrecedingWord()
            if (preferencesState.learnedWordsEnabled && primary.text.isNotBlank()) {
                userDictionaryRepo.recordWord(primary.text.trim(), prevWord, isGesture = true)
            }
            finishComposing()
            ic?.commitText(textToCommit, 1)
            lastGestureCommittedWord = textToCommit

            activeSuggestions = candidates.take(12).mapIndexed { idx, cand ->
                cand.copy(isPrimary = idx == 0)
            }
        }
    }

    private fun updateSuggestions() {
        if (!NativeEngine.isAvailable() || isSymbolsMode) {
            activeSuggestions = emptyList()
            return
        }

        if (isTransliterationLayout()) {
            val translitMgr = getTransliterationManager()
            if (translitMgr.isComposing) {
                val raw = translitMgr.currentRawInput
                val translit = translitMgr.currentTransliteration
                val dictMatches = NativeEngine.querySuggestions(translit, activeLayout.id, 12)
                    .filter { it != translit }

                val list = mutableListOf<SuggestionCandidate>()

                // 1. Raw typed input first in suggestion bar (e.g. "ami", "intarnet")
                if (raw.isNotEmpty()) {
                    list.add(SuggestionCandidate(text = raw, isPrimary = (translit == raw), rawInput = raw))
                }

                // 2. Primary transliterated word
                if (translit.isNotEmpty() && translit != raw) {
                    list.add(SuggestionCandidate(text = translit, isPrimary = true, rawInput = raw))
                }

                // 3. User learned words matching transliterated or raw prefix
                if (preferencesState.learnedWordsEnabled) {
                    val learned = (userDictionaryRepo.getCompletions(translit, 4) + userDictionaryRepo.getCompletions(raw, 4)).distinct()
                    for (w in learned) {
                        if (list.size < 12 && list.none { it.text == w }) {
                            list.add(SuggestionCandidate(text = w, isPrimary = false))
                        }
                    }
                }

                // 4. Additional dictionary completions & alternatives
                for (match in dictMatches) {
                    if (list.size < 12 && list.none { it.text == match }) {
                        list.add(SuggestionCandidate(text = match, isPrimary = false))
                    }
                }

                activeSuggestions = list.take(12)
            } else {
                val prev = getPrecedingWord()
                val learnedNext = if (preferencesState.learnedWordsEnabled && prev.isNotEmpty()) {
                    userDictionaryRepo.getNextWords(prev, 5)
                } else emptyList()
                val predictions = if (prev.isNotEmpty()) NativeEngine.queryNextWords(prev, activeLayout.id, 10) else emptyList()
                val merged = (learnedNext + predictions).distinct().take(12)
                activeSuggestions = merged.mapIndexed { idx, word ->
                    SuggestionCandidate(text = word, isPrimary = idx == 0)
                }
            }
            return
        }

        when (activeLayout.id) {
            "bn_probhat" -> {
                val prefix = composingBuffer.toString()
                if (prefix.isNotEmpty()) {
                    val learned = if (preferencesState.learnedWordsEnabled) {
                        userDictionaryRepo.getCompletions(prefix, 4)
                    } else emptyList()
                    val matches = NativeEngine.querySuggestions(prefix, activeLayout.id, 12)
                    val merged = (learned + matches).distinct().take(12)
                    activeSuggestions = if (merged.isNotEmpty()) {
                        merged.mapIndexed { i, w ->
                            SuggestionCandidate(text = w, isPrimary = i == 0)
                        }
                    } else {
                        listOf(SuggestionCandidate(text = prefix, isPrimary = true))
                    }
                } else {
                    val prev = getPrecedingWord()
                    val learnedNext = if (preferencesState.learnedWordsEnabled && prev.isNotEmpty()) {
                        userDictionaryRepo.getNextWords(prev, 5)
                    } else emptyList()
                    val predictions = if (prev.isNotEmpty()) NativeEngine.queryNextWords(prev, activeLayout.id, 10) else emptyList()
                    val merged = (learnedNext + predictions).distinct().take(12)
                    activeSuggestions = merged.mapIndexed { idx, word ->
                        SuggestionCandidate(text = word, isPrimary = idx == 0)
                    }
                }
            }
            "en_qwerty" -> {
                val prefix = composingBuffer.toString()
                if (prefix.isNotEmpty()) {
                    val learned = if (preferencesState.learnedWordsEnabled) {
                        userDictionaryRepo.getCompletions(prefix, 4)
                    } else emptyList()
                    val matches = NativeEngine.querySuggestions(prefix, activeLayout.id, 12)
                    val merged = (learned + matches).distinct().take(12)
                    activeSuggestions = if (merged.isNotEmpty()) {
                        merged.mapIndexed { i, w ->
                            SuggestionCandidate(text = w, isPrimary = i == 0)
                        }
                    } else {
                        listOf(SuggestionCandidate(text = prefix, isPrimary = true))
                    }
                } else {
                    val prev = getPrecedingWord()
                    val learnedNext = if (preferencesState.learnedWordsEnabled && prev.isNotEmpty()) {
                        userDictionaryRepo.getNextWords(prev, 5)
                    } else emptyList()
                    val predictions = if (prev.isNotEmpty()) NativeEngine.queryNextWords(prev, activeLayout.id, 10) else emptyList()
                    val merged = (learnedNext + predictions).distinct().take(12)
                    activeSuggestions = merged.mapIndexed { idx, word ->
                        SuggestionCandidate(text = word, isPrimary = idx == 0)
                    }
                }
            }
            else -> {
                activeSuggestions = emptyList()
            }
        }
    }

    private fun sendNavKeyEvent(
        ic: InputConnection,
        keyCode: Int,
        isSelecting: Boolean,
        isWord: Boolean = false
    ) {
        val now = SystemClock.uptimeMillis()
        var meta = 0
        if (isSelecting) {
            meta = meta or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        }
        if (isWord) {
            meta = meta or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        }
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, meta))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, meta))
    }
}
