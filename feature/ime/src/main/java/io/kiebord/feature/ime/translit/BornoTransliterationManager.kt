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
    private var cachedTransliteration: String = ""
    private var isTransliterationDirty: Boolean = false

    override val isComposing: Boolean
        get() = buffer.isNotEmpty()

    override fun handleCharacter(char: String): Boolean {
        if (!NativeEngine.isAvailable()) {
            return false
        }

        val ic = inputConnectionProvider() ?: return false
        buffer.append(char)
        val transliterated = NativeEngine.transliterate(layoutId, buffer.toString())
        cachedTransliteration = transliterated
        isTransliterationDirty = false
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
            cachedTransliteration = ""
            isTransliterationDirty = false
            ic.setComposingText("", 1)
            ic.finishComposingText()
        } else {
            val transliterated = if (NativeEngine.isAvailable()) {
                NativeEngine.transliterate(layoutId, buffer.toString())
            } else {
                buffer.toString()
            }
            cachedTransliteration = transliterated
            isTransliterationDirty = false
            ic.setComposingText(transliterated, 1)
        }
        return true
    }

    override val currentRawInput: String
        get() = buffer.toString()

    override val currentTransliteration: String
        get() {
            if (buffer.isEmpty()) return ""
            if (isTransliterationDirty) {
                cachedTransliteration = if (NativeEngine.isAvailable()) {
                    NativeEngine.transliterate(layoutId, buffer.toString())
                } else {
                    buffer.toString()
                }
                isTransliterationDirty = false
            }
            return cachedTransliteration
        }

    override fun commitCustom(text: String) {
        val ic = inputConnectionProvider()
        ic?.commitText(text, 1)
        buffer.clear()
        cachedTransliteration = ""
        isTransliterationDirty = false
    }

    override fun finishComposing() {
        if (!isComposing) return
        val ic = inputConnectionProvider()
        ic?.finishComposingText()
        buffer.clear()
        cachedTransliteration = ""
        isTransliterationDirty = false
    }

    override fun reset() {
        buffer.clear()
        cachedTransliteration = ""
        isTransliterationDirty = false
    }
}

/**
 * Borno phonetic transliteration manager.
 */
class BornoTransliterationManager(
    inputConnectionProvider: () -> InputConnection?
) : GenericTransliterationManager("bn_borno", inputConnectionProvider)

object TransliterationManagerFactory {
    private val phoneticLayoutCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    fun isTransliterationLayout(layoutId: String): Boolean {
        phoneticLayoutCache[layoutId]?.let { return it }
        return if (NativeEngine.isAvailable()) {
            val res = NativeEngine.isPhoneticLayout(layoutId)
            phoneticLayoutCache[layoutId] = res
            res
        } else {
            layoutId == "bn_borno" || layoutId == "bn_avro"
        }
    }

    fun create(layoutId: String, inputConnectionProvider: () -> InputConnection?): TransliterationManager {
        return GenericTransliterationManager(layoutId, inputConnectionProvider)
    }
}
