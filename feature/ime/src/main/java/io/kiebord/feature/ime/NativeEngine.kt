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
    external fun getSuggestions(prefix: String, lang: String, maxResults: Int): String

    @JvmStatic
    external fun predictNextWords(prevWord: String, lang: String, maxResults: Int): String

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
