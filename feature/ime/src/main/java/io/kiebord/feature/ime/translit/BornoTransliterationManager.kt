package io.kiebord.feature.ime.translit

import android.view.inputmethod.InputConnection
import io.kiebord.feature.ime.NativeEngine

class BornoTransliterationManager(
    private val inputConnectionProvider: () -> InputConnection?
) {
    private val buffer = StringBuilder()

    val isComposing: Boolean
        get() = buffer.isNotEmpty()

    fun handleCharacter(char: String): Boolean {
        if (!NativeEngine.isAvailable()) {
            return false
        }

        val ic = inputConnectionProvider() ?: return false
        buffer.append(char)
        val transliterated = NativeEngine.transliterateBorno(buffer.toString())
        ic.setComposingText(transliterated, 1)
        return true
    }

    fun handleDelete(): Boolean {
        if (!isComposing) return false
        val ic = inputConnectionProvider() ?: return false

        buffer.deleteCharAt(buffer.length - 1)
        if (buffer.isEmpty()) {
            ic.setComposingText("", 1)
            ic.finishComposingText()
        } else {
            val transliterated = if (NativeEngine.isAvailable()) {
                NativeEngine.transliterateBorno(buffer.toString())
            } else {
                buffer.toString()
            }
            ic.setComposingText(transliterated, 1)
        }
        return true
    }

    fun finishComposing() {
        if (!isComposing) return
        val ic = inputConnectionProvider()
        ic?.finishComposingText()
        buffer.clear()
    }

    fun reset() {
        buffer.clear()
    }
}
