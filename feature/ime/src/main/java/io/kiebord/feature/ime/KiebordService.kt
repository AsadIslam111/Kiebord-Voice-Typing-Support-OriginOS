package io.kiebord.feature.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.Window
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import io.kiebord.common.model.KeyDef
import io.kiebord.common.model.KeyType
import io.kiebord.common.model.KeyboardLayoutDef
import io.kiebord.common.model.ShiftState
import io.kiebord.data.layout.PredefinedLayouts
import io.kiebord.feature.ime.lifecycle.KeyboardViewLifecycleOwner
import io.kiebord.feature.ime.translit.BornoTransliterationManager
import io.kiebord.feature.ime.ui.KeyboardRootView
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

    override fun onCreate() {
        super.onCreate()
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
        }
        return ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(lifecycleOwner))
            lifecycleOwner.attachToView(this)
            setContent {
                KiebordTheme(colorScheme = colorScheme) {
                    KeyboardRootView(
                        layout = activeLayout,
                        shiftState = shiftState,
                        onKeyTap = ::handleKeyTap,
                        onKeyLongPress = ::handleKeyLongPress,
                        onThemeToggle = {
                            colorScheme = when (colorScheme) {
                                KeyboardColorScheme.DefaultDark -> KeyboardColorScheme.AmoledBlack
                                KeyboardColorScheme.AmoledBlack -> KeyboardColorScheme.DefaultLight
                                else -> KeyboardColorScheme.DefaultDark
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        window?.window?.decorView?.let { decorView ->
            lifecycleOwner.attachToView(decorView)
        }
        lifecycleOwner.onStart()
        shiftState = ShiftState.UNSHIFTED
        isSymbolsMode = false
        activeLayout = primaryAlphaLayout
        bornoManager.reset()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        bornoManager.finishComposing()
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
                    return
                }

                val selectedText = ic.getSelectedText(0)
                if (selectedText.isNullOrEmpty()) {
                    ic.deleteSurroundingText(1, 0)
                } else {
                    ic.commitText("", 1)
                }
            }
            KeyType.ACTION -> {
                bornoManager.finishComposing()
                val currentAction = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                if (currentAction != null && currentAction != EditorInfo.IME_ACTION_NONE && currentAction != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(currentAction)
                } else {
                    sendKeyChar('\n')
                }
            }
            KeyType.SPACE -> {
                bornoManager.finishComposing()
                ic.commitText(" ", 1)
            }
            KeyType.SYMBOLS -> {
                bornoManager.finishComposing()
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
}
