package io.kiebord.feature.ime.translit

import android.view.inputmethod.InputConnection
import io.kiebord.feature.ime.NativeEngine

/**
 * Common interface for layout transliteration managers.
 */
interface TransliterationManager {
    val layoutId: String
    val isComposing: Boolean
    val currentRawInput: String
    val currentTransliteration: String
    fun handleCharacter(char: String): Boolean
    fun handleDelete(): Boolean
    fun commitCustom(text: String)
    fun finishComposing()
    fun reset()
}

/**
 * Generic transliteration manager supporting any phonetic/transliteration layout.
 */
open class GenericTransliterationManager(
    override val layoutId: String,
    private val inputConnectionProvider: () -> InputConnection?
) : TransliterationManager {
    protected val buffer = StringBuilder()

    override val isComposing: Boolean
        get() = buffer.isNotEmpty()

    override fun handleCharacter(char: String): Boolean {
        if (!NativeEngine.isAvailable()) {
            return false
        }

        val ic = inputConnectionProvider() ?: return false
        buffer.append(char)
        val transliterated = NativeEngine.transliterate(layoutId, buffer.toString())
        ic.setComposingText(transliterated, 1)
        return true
    }

    override fun handleDelete(): Boolean {
        if (!isComposing) return false
        val ic = inputConnectionProvider() ?: return false

        if (buffer.isNotEmpty()) {
            val codePoint = buffer.codePointBefore(buffer.length)
            val charCount = Character.charCount(codePoint)
            buffer.delete(buffer.length - charCount, buffer.length)
        }
        if (buffer.isEmpty()) {
            ic.setComposingText("", 1)
            ic.finishComposingText()
        } else {
            val transliterated = if (NativeEngine.isAvailable()) {
                NativeEngine.transliterate(layoutId, buffer.toString())
            } else {
                buffer.toString()
            }
            ic.setComposingText(transliterated, 1)
        }
        return true
    }

    override val currentRawInput: String
        get() = buffer.toString()

    override val currentTransliteration: String
        get() = if (buffer.isNotEmpty() && NativeEngine.isAvailable()) {
            NativeEngine.transliterate(layoutId, buffer.toString())
        } else {
            buffer.toString()
        }

    override fun commitCustom(text: String) {
        val ic = inputConnectionProvider()
        ic?.commitText(text, 1)
        buffer.clear()
    }

    override fun finishComposing() {
        if (!isComposing) return
        val ic = inputConnectionProvider()
        ic?.finishComposingText()
        buffer.clear()
    }

    override fun reset() {
        buffer.clear()
    }
}

/**
 * Borno phonetic transliteration manager.
 */
class BornoTransliterationManager(
    inputConnectionProvider: () -> InputConnection?
) : GenericTransliterationManager("bn_borno", inputConnectionProvider)

object TransliterationManagerFactory {
    fun isTransliterationLayout(layoutId: String): Boolean {
        return if (NativeEngine.isAvailable()) {
            NativeEngine.isPhoneticLayout(layoutId)
        } else {
            layoutId == "bn_borno"
        }
    }

    fun create(layoutId: String, inputConnectionProvider: () -> InputConnection?): TransliterationManager {
        return GenericTransliterationManager(layoutId, inputConnectionProvider)
    }
}
