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
}
