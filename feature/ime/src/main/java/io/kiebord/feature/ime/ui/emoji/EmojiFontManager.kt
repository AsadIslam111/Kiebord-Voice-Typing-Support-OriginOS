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
     * Initializes EmojiCompat asynchronously in the background so font tables are warm
     * without bundling bulky font binaries or ballooning heap memory.
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
        listOf("smile", "happy", "joy", "grin", "glad", "laugh", "cheerful", "haha", "lol") to listOf("😊", "😄", "😃", "😁", "😆", "😂", "🤣", "🙂", "😉", "😇", "😋", "😜", "😝", "😛"),
        listOf("love", "heart", "like", "val", "romance", "crush", "kiss") to listOf("❤️", "💖", "😍", "🥰", "😘", "💕", "💓", "💘", "💌", "🌹", "💋", "😚", "😗"),
        listOf("fire", "hot", "lit", "burn", "flame") to listOf("🔥", "✨", "💥", "⚡", "🌟", "☀️", "🌶️"),
        listOf("cat", "pet", "meow", "kitty") to listOf("🐱", "🐈", "😻", "😽", "😸", "🐾"),
        listOf("dog", "pup", "bark", "woof") to listOf("🐶", "🐕", "🐩", "🦮", "🐾", "🦴"),
        listOf("cry", "sad", "tear", "unhappy", "sob") to listOf("😢", "😭", "🥺", "😿", "💔", "😞", "😔", "😥", "😩", "😫"),
        listOf("cool", "sunglasses", "swag") to listOf("😎", "🆒", "🕶️", "🤙", "😏", "✌️"),
        listOf("food", "eat", "hungry", "snack", "yum") to listOf("🍕", "🍔", "🍟", "🌮", "🍦", "🍩", "🍰", "🍣", "🍜", "🍿", "🍗", "🥪", "🍳"),
        listOf("car", "drive", "ride", "auto", "vehicle") to listOf("🚗", "🏎️", "🚙", "🚕", "🚌", "🚘", "🏍️", "🚲"),
        listOf("clap", "bravo", "congrats", "applause") to listOf("👏", "🙌", "🎉", "🥳", "✨", "🎊", "🏆"),
        listOf("star", "glow", "night", "moon", "sky") to listOf("⭐", "🌟", "✨", "💫", "🌙", "🌠", "🌌"),
        listOf("ok", "good", "yes", "thumbs", "up") to listOf("👍", "👌", "✅", "🙌", "💯", "🤝", "✔️"),
        listOf("no", "bad", "thumbsdown", "down", "dislike") to listOf("👎", "❌", "🚫", "🛑", "⛔"),
        listOf("party", "celebrate", "dance") to listOf("🥳", "🎉", "🎊", "🍾", "💃", "🕺", "🎈", "🍻"),
        listOf("sleep", "tired", "rest", "bed", "yawn") to listOf("😴", "💤", "🥱", "🛌", "🌙", "😪"),
        listOf("money", "cash", "rich", "dollar", "wealth") to listOf("💰", "💵", "🤑", "💸", "💳", "🪙", "💎"),
        listOf("hand", "wave", "hi", "hello", "bye") to listOf("👋", "✋", "🤚", "🖐️", "🙋‍♂️", "🙋‍♀️"),
        listOf("pray", "please", "thanks", "thank", "namaste") to listOf("🙏", "🤲", "🙇‍♂️", "🙇‍♀️"),
        listOf("think", "thinking", "hmm", "idea", "smart") to listOf("🤔", "🧐", "💡", "🧠", "💭"),
        listOf("angry", "mad", "rage", "furious") to listOf("😠", "😡", "🤬", "😤", "👿", "💢"),
        listOf("shock", "omg", "wow", "scared", "fear", "surprised") to listOf("😱", "😲", "🤯", "😳", "😨", "😰", "😯"),
        listOf("skull", "dead", "rip", "ghost") to listOf("💀", "☠️", "👻", "🎃"),
        listOf("sick", "ill", "mask", "fever", "hurt", "hospital") to listOf("🤒", "😷", "🤕", "🤢", "🤮", "🤧", "💊", "💉"),
        listOf("sun", "summer", "warm", "weather", "cloud", "rain") to listOf("☀️", "🌤️", "⛅", "🌧️", "⚡", "🌈", "❄️"),
        listOf("coffee", "tea", "drink", "cup", "cafe", "beer", "wine") to listOf("☕", "🍵", "🧋", "🥤", "🍺", "🍻", "🍷", "🥂"),
        listOf("music", "song", "audio", "sound", "guitar") to listOf("🎵", "🎶", "🎧", "🎤", "🎸", "🎹", "🥁"),
        listOf("flower", "rose", "nature", "plant", "garden") to listOf("🌹", "🌸", "🌺", "🌻", "🌼", "🌷", "🌿", "🍀"),
        listOf("game", "play", "win", "gamer") to listOf("🎮", "🕹️", "🎲", "🎯", "🏆", "🥇"),
        listOf("bd", "bangla", "bangladesh", "flag") to listOf("🇧🇩", "🏁", "🚩")
    )

    private val defaultCandidates = listOf(
        "😊", "❤️", "🔥", "👍", "🎉", "😂", "✨", "😍", "🙌", "😎", "🥳", "🍕",
        "👏", "💯", "💖", "🥰", "😭", "🥺", "😉", "🤩", "⭐", "💪", "🚀", "👌",
        "🤤", "😜", "😴", "🤔", "👀", "🤝", "🎶", "☕", "🤣", "💀", "😇", "😋",
        "🤗", "🤭", "🤫", "😏", "😬", "🙄", "😱", "🤠", "🤑", "👋", "🙏", "🍻"
    )

    /**
     * Searches emoji catalog matching query keywords, returning candidates.
     */
    fun search(query: String): List<String> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return defaultCandidates

        val results = mutableListOf<String>()
        for ((keywords, emojis) in searchData) {
            if (keywords.any { it.startsWith(q) || q.startsWith(it) || it.contains(q) }) {
                results.addAll(emojis)
            }
        }
        return results.distinct()
    }
}
