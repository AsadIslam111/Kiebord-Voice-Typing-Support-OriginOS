package com.codepotro.borno.feature.ime.ui.emoji

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.ContextCompat
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
     * Purges any cached AndroidX EmojiPicker files (both regular and device-protected storage)
     * and resets in-memory singletons so overridden raw CSV resources take effect immediately.
     */
    fun clearEmojiPickerCache(context: Context) {
        try {
            val dpsContext = ContextCompat.createDeviceProtectedStorageContext(context)
            val candidateDirs = listOfNotNull(
                File(context.cacheDir, "emoji_picker"),
                File(context.filesDir, "emoji_picker"),
                File(context.noBackupFilesDir, "emoji_picker"),
                dpsContext?.let { File(it.cacheDir, "emoji_picker") },
                dpsContext?.let { File(it.filesDir, "emoji_picker") },
                dpsContext?.let { File(it.noBackupFilesDir, "emoji_picker") }
            )
            candidateDirs.forEach { dir ->
                if (dir.exists()) {
                    dir.deleteRecursively()
                }
            }
        } catch (_: Exception) {}

        // Reset BundledEmojiListLoader in-memory caches if already loaded
        try {
            val loaderClass = Class.forName("androidx.emoji2.emojipicker.BundledEmojiListLoader")
            val catField = loaderClass.getDeclaredField("categorizedEmojiData").apply { isAccessible = true }
            catField.set(null, null)
            val mapField = loaderClass.getDeclaredField("emojiVariantsLookup").apply { isAccessible = true }
            mapField.set(null, null)
        } catch (_: Exception) {}
    }

    /**
     * Initializes EmojiCompat asynchronously in the background so font tables are warm
     * without bundling bulky font binaries or ballooning heap memory.
     */
    fun initEmojiCompat(context: Context) {
        if (isInitialized) return
        isInitialized = true
        clearEmojiPickerCache(context)
        EmojiSearchCatalog.init(context)
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

