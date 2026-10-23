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
    external fun transliterateBorno(input: String): String

    @JvmStatic
    external fun getSuggestions(prefix: String, lang: String, maxResults: Int): String

    fun querySuggestions(prefix: String, lang: String, maxResults: Int = 5): List<String> {
        if (!isLoaded) return emptyList()
        return try {
            val raw = getSuggestions(prefix, lang, maxResults)
            if (raw.isEmpty()) emptyList() else raw.split("|").filter { it.isNotEmpty() }
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
