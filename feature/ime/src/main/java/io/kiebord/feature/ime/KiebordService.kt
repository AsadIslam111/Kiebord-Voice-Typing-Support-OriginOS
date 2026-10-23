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
import androidx.core.view.WindowCompat
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
                        bornoManager.finishComposing()
                        activeSuggestions = emptyList()
                        ic?.commitText(clipText, 1)
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
        activeSuggestions = emptyList()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        bornoManager.finishComposing()
        activeSuggestions = emptyList()
        lifecycleOwner.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleOwner.onDestroy()
    }

    private fun handleKeyTap(key: KeyDef) {
        val ic = currentInputConnection ?: return

        when (key.type) {
            KeyType.CHARACTER -> {
                val text = key.displayLabel(shiftState)

                // If in Borno Phonetic mode and typing letters, stream through transliteration manager
                if (activeLayout.id == "bn_borno" && isTransliterableChar(text)) {
                    bornoManager.handleCharacter(text)
                } else {
                    bornoManager.finishComposing()
                    ic.commitText(text, 1)
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
                updateSuggestions()

                val selectedText = ic.getSelectedText(0)
                if (selectedText.isNullOrEmpty()) {
                    ic.deleteSurroundingText(1, 0)
                } else {
                    ic.commitText("", 1)
                }
            }
            KeyType.ACTION -> {
                bornoManager.finishComposing()
                activeSuggestions = emptyList()
                val currentAction = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                if (currentAction != null && currentAction != EditorInfo.IME_ACTION_NONE && currentAction != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(currentAction)
                } else {
                    sendKeyChar('\n')
                }
            }
            KeyType.SPACE -> {
                bornoManager.finishComposing()
                activeSuggestions = emptyList()
                ic.commitText(" ", 1)
            }
            KeyType.SYMBOLS -> {
                bornoManager.finishComposing()
                activeSuggestions = emptyList()
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
                        // Fallback toggle
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
            }
            KeyType.SWITCH_LANGUAGE -> {
                bornoManager.finishComposing()
                // Cycle between English QWERTY -> Bengali Borno -> Bengali Probhat -> English QWERTY
                primaryAlphaLayout = when (primaryAlphaLayout.id) {
                    "en_qwerty" -> PredefinedLayouts.BengaliBorno
                    "bn_borno" -> PredefinedLayouts.BengaliProbhat
                    else -> PredefinedLayouts.EnglishQwerty
                }
                if (!isSymbolsMode) {
                    activeLayout = primaryAlphaLayout
                }
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
        bornoManager.finishComposing()
        if (!key.flickUp.isNullOrEmpty()) {
            ic.commitText(key.flickUp!!, 1)
        } else if (key.moreKeys.isNotEmpty()) {
            ic.commitText(key.moreKeys.first(), 1)
        }
    }

    private fun isTransliterableChar(text: String): Boolean {
        if (text.length != 1) return false
        val c = text[0]
        return (c in 'a'..'z') || (c in 'A'..'Z')
    }

    private fun handleTextAction(action: TextAction, isSelecting: Boolean) {
        val ic = currentInputConnection ?: return
        bornoManager.finishComposing()

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
    }

    private fun handleCandidateSelect(candidate: SuggestionCandidate) {
        val ic = currentInputConnection ?: return
        bornoManager.commitCustom(candidate.text)
        activeSuggestions = emptyList()
    }

    private fun updateSuggestions() {
        if (bornoManager.isComposing) {
            val bengali = bornoManager.currentTransliteration
            val raw = bornoManager.currentRawInput
            activeSuggestions = listOf(
                SuggestionCandidate(text = bengali, isPrimary = true, rawInput = raw),
                SuggestionCandidate(text = raw, isPrimary = false, rawInput = raw)
            )
        } else {
            activeSuggestions = emptyList()
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

