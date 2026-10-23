package io.kiebord.feature.ime

import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
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

    override fun onCreate() {
        super.onCreate()
        preferencesRepo = KiebordPreferencesRepository.getInstance(this)
        clipboardManager = SandboxedClipboardManager(this)
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
                val targetPrimary = when (prefs.primaryLayoutId) {
                    KiebordPreferences.LAYOUT_BN_BORNO -> PredefinedLayouts.BengaliBorno
                    KiebordPreferences.LAYOUT_BN_PROBHAT -> PredefinedLayouts.BengaliProbhat
                    else -> PredefinedLayouts.EnglishQwerty
                }
                if (primaryAlphaLayout.id != targetPrimary.id) {
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

            KiebordTheme(colorScheme = colorScheme) {
                KeyboardRootView(
                    layout = activeLayout,
                    shiftState = shiftState,
                    navBarBottomInset = navBarBottomDp,
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
                    initialRowHeight = (60 * preferencesState.keyboardHeightScale).dp,
                    defaultShowNumberRow = preferencesState.showNumberRow,
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

                val selectedText = ic.getSelectedText(0)
                if (selectedText.isNullOrEmpty()) {
                    ic.deleteSurroundingText(1, 0)
                } else {
                    ic.commitText("", 1)
                }
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
                    ic.commitText(translitMgr.currentTransliteration + " ", 1)
                    translitMgr.reset()
                } else if (composingBuffer.isNotEmpty()) {
                    ic.commitText(composingBuffer.toString() + " ", 1)
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
                // Cycle between English QWERTY -> Bengali Borno -> Bengali Probhat -> English QWERTY
                primaryAlphaLayout = when (primaryAlphaLayout.id) {
                    "en_qwerty" -> PredefinedLayouts.BengaliBorno
                    "bn_borno" -> PredefinedLayouts.BengaliProbhat
                    else -> PredefinedLayouts.EnglishQwerty
                }
                if (!isSymbolsMode) {
                    activeLayout = primaryAlphaLayout
                }
                updateSuggestions()
            }
            KeyType.MORE_KEYS -> {
                // Secondary popups
            }
        }
    }

    private fun handleKeyLongPress(key: KeyDef) {
        if (key.type == KeyType.SPACE) {
            handleKeyTap(KeyDef(type = KeyType.SWITCH_LANGUAGE))
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

    private fun handleLongPressDelete() {
        if (!preferencesState.longPressDeleteEnabled) {
            handleKeyTap(KeyDef(type = KeyType.DELETE))
            return
        }
        val ic = currentInputConnection ?: return
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
        if (composingBuffer.isNotEmpty()) {
            if (preferencesState.selectiveDeleteMode == KiebordPreferences.DELETE_MODE_WORD) {
                composingBuffer.clear()
                ic.setComposingText("", 1)
                ic.finishComposingText()
            } else {
                composingBuffer.deleteCharAt(composingBuffer.length - 1)
                if (composingBuffer.isEmpty()) {
                    ic.setComposingText("", 1)
                    ic.finishComposingText()
                } else {
                    ic.setComposingText(composingBuffer.toString(), 1)
                }
            }
            updateSuggestions()
            return
        }

        if (preferencesState.selectiveDeleteMode == KiebordPreferences.DELETE_MODE_WORD) {
            val textBefore = ic.getTextBeforeCursor(64, 0)
            if (!textBefore.isNullOrEmpty()) {
                var idx = textBefore.length
                while (idx > 0 && textBefore[idx - 1].isWhitespace()) {
                    idx--
                }
                while (idx > 0 && !textBefore[idx - 1].isWhitespace()) {
                    idx--
                }
                val charsToDelete = textBefore.length - idx
                if (charsToDelete > 0) {
                    ic.deleteSurroundingText(charsToDelete, 0)
                } else {
                    ic.deleteSurroundingText(1, 0)
                }
            } else {
                ic.deleteSurroundingText(1, 0)
            }
        } else {
            val selectedText = ic.getSelectedText(0)
            if (selectedText.isNullOrEmpty()) {
                ic.deleteSurroundingText(1, 0)
            } else {
                ic.commitText("", 1)
            }
        }
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

    private fun getPrecedingWord(): String {
        val ic = currentInputConnection ?: return ""
        val text = ic.getTextBeforeCursor(40, 0)?.toString() ?: return ""
        val trimmed = text.trimEnd()
        if (trimmed.isEmpty()) return ""
        return trimmed.split(Regex("[\\s,.;!?]+")).lastOrNull() ?: ""
    }

    private fun handleCandidateSelect(candidate: SuggestionCandidate) {
        val ic = currentInputConnection ?: return
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

        val candidates = gestureDecoder.decodeGesture(trail, centroids, keyWidthPx, lang)
        if (candidates.isNotEmpty()) {
            val primary = candidates.first()
            val textToCommit = primary.text + " "
            val ic = currentInputConnection
            finishComposing()
            ic?.commitText(textToCommit, 1)
            lastGestureCommittedWord = textToCommit

            activeSuggestions = candidates.take(3).mapIndexed { idx, cand ->
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
                val dictLang = if (activeLayout.id.startsWith("bn")) "bn" else "en"
                val dictMatches = NativeEngine.querySuggestions(translit, dictLang, 5)
                    .filter { it != translit }

                val list = mutableListOf<SuggestionCandidate>()
                // Primary transliterated word
                list.add(SuggestionCandidate(text = translit, isPrimary = true, rawInput = raw))

                // Dictionary matches in descending frequency order
                for (match in dictMatches) {
                    if (list.size >= 3) break
                    list.add(SuggestionCandidate(text = match, isPrimary = false))
                }

                // Fallback to raw English input if room remains and differs
                if (list.size < 3 && raw.isNotEmpty() && raw != translit && list.none { it.text == raw }) {
                    list.add(SuggestionCandidate(text = raw, isPrimary = false, rawInput = raw))
                }

                activeSuggestions = list
            } else {
                val prev = getPrecedingWord()
                val dictLang = if (activeLayout.id.startsWith("bn")) "bn" else "en"
                val predictions = if (prev.isNotEmpty()) NativeEngine.queryNextWords(prev, dictLang, 3) else emptyList()
                activeSuggestions = predictions.take(3).mapIndexed { idx, word ->
                    SuggestionCandidate(text = word, isPrimary = idx == 0)
                }
            }
            return
        }

        when (activeLayout.id) {
            "bn_probhat" -> {
                val prefix = composingBuffer.toString()
                if (prefix.isNotEmpty()) {
                    val matches = NativeEngine.querySuggestions(prefix, "bn", 5)
                    activeSuggestions = if (matches.isNotEmpty()) {
                        matches.take(3).mapIndexed { i, w ->
                            SuggestionCandidate(text = w, isPrimary = i == 0)
                        }
                    } else {
                        listOf(SuggestionCandidate(text = prefix, isPrimary = true))
                    }
                } else {
                    val prev = getPrecedingWord()
                    val predictions = if (prev.isNotEmpty()) NativeEngine.queryNextWords(prev, "bn", 3) else emptyList()
                    activeSuggestions = predictions.take(3).mapIndexed { idx, word ->
                        SuggestionCandidate(text = word, isPrimary = idx == 0)
                    }
                }
            }
            "en_qwerty" -> {
                val prefix = composingBuffer.toString()
                if (prefix.isNotEmpty()) {
                    val matches = NativeEngine.querySuggestions(prefix, "en", 5)
                    activeSuggestions = if (matches.isNotEmpty()) {
                        matches.take(3).mapIndexed { i, w ->
                            SuggestionCandidate(text = w, isPrimary = i == 0)
                        }
                    } else {
                        listOf(SuggestionCandidate(text = prefix, isPrimary = true))
                    }
                } else {
                    val prev = getPrecedingWord()
                    val predictions = if (prev.isNotEmpty()) NativeEngine.queryNextWords(prev, "en", 3) else emptyList()
                    activeSuggestions = predictions.take(3).mapIndexed { idx, word ->
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
