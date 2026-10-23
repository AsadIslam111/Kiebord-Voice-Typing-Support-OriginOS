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
import io.kiebord.feature.ime.lifecycle.KeyboardViewLifecycleOwner
import io.kiebord.feature.ime.translit.BornoTransliterationManager
import io.kiebord.feature.ime.ui.KeyboardRootView
import io.kiebord.feature.ime.ui.suggestion.SuggestionCandidate
import io.kiebord.feature.ime.ui.textedit.TextAction
import io.kiebord.theme.KeyboardColorScheme
import io.kiebord.theme.KiebordTheme

class KiebordService : InputMethodService() {

    private val lifecycleOwner = KeyboardViewLifecycleOwner()
    private val bornoManager = BornoTransliterationManager { currentInputConnection }
    private val composingBuffer = StringBuilder()

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
        clipboardManager = SandboxedClipboardManager(this)
        lifecycleOwner.onCreate()
        NativeEngine.ensureInitialized(this, "en")
        NativeEngine.ensureInitialized(this, "bn")
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
                        ic?.commitText(clipText, 1)
                        updateSuggestions()
                    },
                    onThemeToggle = {
                        colorScheme = when (colorScheme) {
                            KeyboardColorScheme.DefaultDark -> KeyboardColorScheme.AmoledBlack
                            KeyboardColorScheme.AmoledBlack -> KeyboardColorScheme.TokyoNight
                            KeyboardColorScheme.TokyoNight -> KeyboardColorScheme.Nord
                            KeyboardColorScheme.Nord -> KeyboardColorScheme.DefaultLight
                            else -> KeyboardColorScheme.DefaultDark
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
        bornoManager.reset()
        composingBuffer.clear()
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
        lifecycleOwner.onDestroy()
    }

    private fun finishComposing() {
        val ic = currentInputConnection
        if (bornoManager.isComposing) {
            ic?.commitText(bornoManager.currentTransliteration, 1)
            bornoManager.reset()
        }
        if (composingBuffer.isNotEmpty()) {
            ic?.commitText(composingBuffer.toString(), 1)
            composingBuffer.clear()
        }
        ic?.finishComposingText()
    }

    private fun handleKeyTap(key: KeyDef) {
        val ic = currentInputConnection ?: return

        when (key.type) {
            KeyType.CHARACTER -> {
                val text = key.displayLabel(shiftState)

                when {
                    // Borno Phonetic mode
                    activeLayout.id == "bn_borno" -> {
                        if (isTransliterableChar(text)) {
                            bornoManager.handleCharacter(text)
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
                // If currently composing in Borno phonetic, delete within the composing buffer first
                if (bornoManager.handleDelete()) {
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
                if (bornoManager.isComposing) {
                    ic.commitText(bornoManager.currentTransliteration + " ", 1)
                    bornoManager.reset()
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
        val ic = currentInputConnection ?: return
        finishComposing()
        if (!key.flickUp.isNullOrEmpty()) {
            ic.commitText(key.flickUp!!, 1)
        } else if (key.moreKeys.isNotEmpty()) {
            ic.commitText(key.moreKeys.first(), 1)
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
        if (bornoManager.isComposing) {
            bornoManager.commitCustom(candidate.text + " ")
        } else if (composingBuffer.isNotEmpty()) {
            ic.commitText(candidate.text + " ", 1)
            composingBuffer.clear()
        } else {
            ic.commitText(candidate.text + " ", 1)
        }
        updateSuggestions()
    }

    private fun updateSuggestions() {
        if (!NativeEngine.isAvailable() || isSymbolsMode) {
            activeSuggestions = emptyList()
            return
        }

        when (activeLayout.id) {
            "bn_borno" -> {
                if (bornoManager.isComposing) {
                    val raw = bornoManager.currentRawInput
                    val translit = bornoManager.currentTransliteration
                    val dictMatches = NativeEngine.querySuggestions(translit, "bn", 5)
                        .filter { it != translit }

                    val list = mutableListOf<SuggestionCandidate>()
                    val nextMatch = dictMatches.firstOrNull()

                    // Left: raw English input if different from transliteration
                    if (raw.isNotEmpty() && raw != translit) {
                        list.add(SuggestionCandidate(text = raw, isPrimary = false, rawInput = raw))
                    } else if (dictMatches.size > 1) {
                        list.add(SuggestionCandidate(text = dictMatches[1], isPrimary = false))
                    }

                    // Center: Transliterated Bengali (primary)
                    list.add(SuggestionCandidate(text = translit, isPrimary = true, rawInput = raw))

                    // Right: Dictionary completion
                    if (nextMatch != null) {
                        list.add(SuggestionCandidate(text = nextMatch, isPrimary = false))
                    }

                    activeSuggestions = list
                } else {
                    val prev = getPrecedingWord()
                    val predictions = if (prev.isNotEmpty()) NativeEngine.queryNextWords(prev, "bn", 3) else emptyList()
                    val candidates = if (predictions.isNotEmpty()) {
                        predictions
                    } else {
                        NativeEngine.querySuggestions("", "bn", 3)
                    }

                    if (candidates.size >= 3) {
                        activeSuggestions = listOf(
                            SuggestionCandidate(text = candidates[1], isPrimary = false),
                            SuggestionCandidate(text = candidates[0], isPrimary = true),
                            SuggestionCandidate(text = candidates[2], isPrimary = false)
                        )
                    } else {
                        activeSuggestions = candidates.mapIndexed { idx, word ->
                            SuggestionCandidate(text = word, isPrimary = idx == 0)
                        }
                    }
                }
            }
            "bn_probhat" -> {
                val prefix = composingBuffer.toString()
                if (prefix.isNotEmpty()) {
                    val matches = NativeEngine.querySuggestions(prefix, "bn", 5)
                    val list = mutableListOf<SuggestionCandidate>()
                    if (matches.isNotEmpty()) {
                        if (matches.size >= 3) {
                            list.add(SuggestionCandidate(text = matches[1], isPrimary = false))
                            list.add(SuggestionCandidate(text = matches[0], isPrimary = true))
                            list.add(SuggestionCandidate(text = matches[2], isPrimary = false))
                        } else {
                            matches.forEachIndexed { i, w ->
                                list.add(SuggestionCandidate(text = w, isPrimary = i == 0))
                            }
                        }
                    } else {
                        list.add(SuggestionCandidate(text = prefix, isPrimary = true))
                    }
                    activeSuggestions = list
                } else {
                    val prev = getPrecedingWord()
                    val predictions = if (prev.isNotEmpty()) NativeEngine.queryNextWords(prev, "bn", 3) else emptyList()
                    val candidates = if (predictions.isNotEmpty()) {
                        predictions
                    } else {
                        NativeEngine.querySuggestions("", "bn", 3)
                    }

                    if (candidates.size >= 3) {
                        activeSuggestions = listOf(
                            SuggestionCandidate(text = candidates[1], isPrimary = false),
                            SuggestionCandidate(text = candidates[0], isPrimary = true),
                            SuggestionCandidate(text = candidates[2], isPrimary = false)
                        )
                    } else {
                        activeSuggestions = candidates.mapIndexed { idx, word ->
                            SuggestionCandidate(text = word, isPrimary = idx == 0)
                        }
                    }
                }
            }
            "en_qwerty" -> {
                val prefix = composingBuffer.toString()
                if (prefix.isNotEmpty()) {
                    val matches = NativeEngine.querySuggestions(prefix, "en", 5)
                    val list = mutableListOf<SuggestionCandidate>()
                    if (matches.isNotEmpty()) {
                        if (matches.size >= 3) {
                            list.add(SuggestionCandidate(text = matches[1], isPrimary = false))
                            list.add(SuggestionCandidate(text = matches[0], isPrimary = true))
                            list.add(SuggestionCandidate(text = matches[2], isPrimary = false))
                        } else {
                            matches.forEachIndexed { i, w ->
                                list.add(SuggestionCandidate(text = w, isPrimary = i == 0))
                            }
                        }
                    } else {
                        list.add(SuggestionCandidate(text = prefix, isPrimary = true))
                    }
                    activeSuggestions = list
                } else {
                    val prev = getPrecedingWord()
                    val predictions = if (prev.isNotEmpty()) NativeEngine.queryNextWords(prev, "en", 3) else emptyList()
                    val candidates = if (predictions.isNotEmpty()) {
                        predictions
                    } else {
                        NativeEngine.querySuggestions("", "en", 3)
                    }

                    if (candidates.size >= 3) {
                        activeSuggestions = listOf(
                            SuggestionCandidate(text = candidates[1], isPrimary = false),
                            SuggestionCandidate(text = candidates[0], isPrimary = true),
                            SuggestionCandidate(text = candidates[2], isPrimary = false)
                        )
                    } else {
                        activeSuggestions = candidates.mapIndexed { idx, word ->
                            SuggestionCandidate(text = word, isPrimary = idx == 0)
                        }
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
