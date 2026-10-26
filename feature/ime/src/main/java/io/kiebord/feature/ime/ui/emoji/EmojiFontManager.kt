package io.kiebord.feature.ime.ui.emoji

import android.content.Context
import android.graphics.Typeface
import androidx.emoji2.text.DefaultEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import java.io.File

/**
 * Manager for custom emoji fonts and EmojiCompat warm-up.
 *
 * Provides a clean abstraction for loading custom .ttf color emoji fonts
 * (e.g., Apple Color Emoji, Twemoji, Noto Color Emoji, Fluent) in the future,
 * while managing asynchronous EmojiCompat initialization for zero-lag emoji rendering.
 */
object EmojiFontManager {
    private var customEmojiFontFile: File? = null
    private var cachedTypeface: Typeface? = null
    private var isInitialized = false

    /**
     * Initializes EmojiCompat asynchronously in the background so font tables are warm.
     */
    fun initEmojiCompat(context: Context) {
        if (isInitialized) return
        isInitialized = true
        try {
            val config = DefaultEmojiCompatConfig.create(context)
            if (config != null) {
                EmojiCompat.init(config.setReplaceAll(true))
            }
        } catch (_: Exception) {}
    }

    /**
     * Returns the active custom emoji Typeface if a custom font is configured and exists.
     */
    fun getCustomEmojiTypeface(): Typeface? {
        val file = customEmojiFontFile ?: return null
        if (cachedTypeface != null) return cachedTypeface
        return if (file.exists()) {
            try {
                val tf = Typeface.createFromFile(file)
                cachedTypeface = tf
                tf
            } catch (_: Exception) {
                null
            }
        } else null
    }

    fun isCustomFontActive(): Boolean = customEmojiFontFile?.exists() == true

    fun setCustomFontFile(file: File?) {
        customEmojiFontFile = file
        cachedTypeface = null
    }
}

/**
 * Catalog for emoji keyword search providing instant candidate matches.
 */
object EmojiSearchCatalog {
    private val searchData: List<Pair<List<String>, List<String>>> = listOf(
        listOf("smile", "happy", "joy", "grin", "glad", "laugh") to listOf("😊", "😄", "😃"),
        listOf("love", "heart", "like", "val", "romance", "crush") to listOf("❤️", "💖", "😍"),
        listOf("fire", "hot", "lit", "burn", "flame") to listOf("🔥", "✨", "💥"),
        listOf("cat", "pet", "meow", "kitty") to listOf("🐱", "🐈", "😻"),
        listOf("dog", "pup", "bark", "woof") to listOf("🐶", "🐕", "🐩"),
        listOf("cry", "sad", "tear", "unhappy") to listOf("😢", "😭", "🥺"),
        listOf("cool", "sunglasses", "swag") to listOf("😎", "🆒", "🕶️"),
        listOf("food", "eat", "hungry", "snack") to listOf("🍕", "🍔", "🍟"),
        listOf("car", "drive", "ride", "auto") to listOf("🚗", "🏎️", "🚙"),
        listOf("clap", "bravo", "congrats") to listOf("👏", "🙌", "🎉"),
        listOf("star", "glow", "night") to listOf("⭐", "🌟", "✨"),
        listOf("ok", "good", "yes", "thumbs") to listOf("👍", "👌", "✅"),
        listOf("party", "celebrate", "dance") to listOf("🥳", "🎉", "🎊"),
        listOf("sleep", "tired", "rest") to listOf("😴", "💤", "🥱"),
        listOf("money", "cash", "rich") to listOf("💰", "💵", "🤑")
    )

    private val defaultCandidates = listOf("😊", "❤️", "🔥")

    /**
     * Searches dummy emoji catalog matching query keywords, returning 3 candidates.
     */
    fun search(query: String): List<String> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return defaultCandidates

        for ((keywords, emojis) in searchData) {
            if (keywords.any { it.startsWith(q) || q.startsWith(it) }) {
                return emojis
            }
        }
        return defaultCandidates
    }
}
