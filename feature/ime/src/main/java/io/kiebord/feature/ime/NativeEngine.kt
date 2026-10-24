package io.kiebord.feature.ime

object NativeEngine {

    private var isLoaded = false

    init {
        try {
            System.loadLibrary("kiebord_engine")
            isLoaded = true
        } catch (_: UnsatisfiedLinkError) {
            isLoaded = false
        } catch (_: SecurityException) {
            isLoaded = false
        }
    }

    fun isAvailable(): Boolean = isLoaded

    @JvmStatic
    external fun ping(): Boolean

    @JvmStatic
    external fun loadDictionary(lang: String, path: String): Boolean

    @JvmStatic
    external fun transliterateBorno(input: String): String

    @JvmStatic
    external fun transliterate(layoutId: String, input: String): String

    @JvmStatic
    external fun isPhoneticLayout(layoutId: String): Boolean

    @JvmStatic
    external fun getSuggestions(prefix: String, lang: String, maxResults: Int): String

    @JvmStatic
    external fun predictNextWords(prevWord: String, lang: String, maxResults: Int): String

    @JvmStatic
    external fun registerLayoutProximityRows(layoutId: String, rows: Array<String>): Boolean

    @JvmStatic
    external fun decodeGesture(
        pointsX: FloatArray,
        pointsY: FloatArray,
        keysChars: String,
        keysX: FloatArray,
        keysY: FloatArray,
        keyWidth: Float,
        lang: String,
        maxResults: Int
    ): String

    fun queryGesture(
        points: List<androidx.compose.ui.geometry.Offset>,
        keyCentroids: List<io.kiebord.feature.ime.gesture.KeyCentroid>,
        keyWidth: Float,
        lang: String,
        maxResults: Int = 5,
        learnedWords: List<String> = emptyList()
    ): List<String> {
        if (!isLoaded || points.size < 2 || keyCentroids.isEmpty()) return emptyList()
        val xs = FloatArray(points.size) { points[it].x }
        val ys = FloatArray(points.size) { points[it].y }
        val chars = StringBuilder()
        val kxs = FloatArray(keyCentroids.size)
        val kys = FloatArray(keyCentroids.size)
        for (i in keyCentroids.indices) {
            val c = keyCentroids[i].character
            chars.append(if (c.isNotEmpty()) c[0] else ' ')
            kxs[i] = keyCentroids[i].centerX
            kys[i] = keyCentroids[i].centerY
        }
        val targetLang = if (learnedWords.isNotEmpty()) {
            val encoded = learnedWords.take(50).joinToString("|")
            "$lang;$encoded"
        } else lang
        return try {
            val raw = decodeGesture(xs, ys, chars.toString(), kxs, kys, keyWidth, targetLang, maxResults)
            if (raw.isEmpty()) emptyList() else raw.split("|").filter { it.isNotEmpty() }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private val initializedLangs = mutableSetOf<String>()

    fun ensureInitialized(context: android.content.Context, lang: String) {
        if (!isLoaded) return
        val baseLang = if (lang.startsWith("bn")) "bn" else "en"
        if (initializedLangs.contains(baseLang)) return

        try {
            val assetPath = "dictionaries/$baseLang.dict"
            val destFile = java.io.File(context.filesDir, assetPath)
            var needsCopy = !destFile.exists()

            if (!needsCopy) {
                try {
                    context.assets.open(assetPath).use { stream ->
                        if (destFile.length() != stream.available().toLong()) {
                            needsCopy = true
                        }
                    }
                } catch (_: Throwable) {}
            }

            if (needsCopy) {
                destFile.parentFile?.mkdirs()
                context.assets.open(assetPath).use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            if (loadDictionary(baseLang, destFile.absolutePath)) {
                initializedLangs.add(baseLang)
            }
        } catch (_: Throwable) {}
    }

    fun querySuggestions(prefix: String, lang: String, maxResults: Int = 5): List<String> {
        if (!isLoaded) return emptyList()
        return try {
            val raw = getSuggestions(prefix, lang, maxResults)
            if (raw.isEmpty()) emptyList() else raw.split("|").filter { it.isNotEmpty() }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun queryNextWords(prevWord: String, lang: String, maxResults: Int = 3): List<String> {
        if (!isLoaded || prevWord.isBlank()) return emptyList()
        return try {
            val raw = predictNextWords(prevWord.trim(), lang, maxResults)
            if (raw.isEmpty()) emptyList() else raw.split("|").filter { it.isNotEmpty() }
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
