package com.codepotro.borno.feature.ime.voice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import java.util.Locale

/**
 * Transparent proxy activity used to launch RecognizerIntent.ACTION_RECOGNIZE_SPEECH.
 *
 * This approach is 100% immune to Chinese ROM (Vivo OriginOS, Xiaomi HyperOS, Oppo ColorOS)
 * background Binder service restrictions, because it executes as a visible foreground Activity.
 */
class VoiceRecognitionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            // Already started and waiting for activity result, do not launch again
            return
        }

        val language = intent.getStringExtra(EXTRA_LANGUAGE) ?: "en"
        val localeTag = getLocaleTagForLanguage(language)

        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf(localeTag))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                if (localeTag.startsWith("bn")) "এখন বলুন..." else "Speak now..."
            )
        }

        // Try explicitly targeting Google's recognition packages if available
        val pm = packageManager
        for (pkg in listOf("com.google.android.tts", "com.google.android.googlequicksearchbox")) {
            val targetedIntent = Intent(speechIntent).setPackage(pkg)
            if (pm.queryIntentActivities(targetedIntent, 0).isNotEmpty()) {
                speechIntent.setPackage(pkg)
                break
            }
        }

        try {
            @Suppress("DEPRECATION")
            startActivityForResult(speechIntent, REQUEST_CODE_SPEECH)
        } catch (e: Exception) {
            VoiceInputManager.onActivityError("Speech recognition not supported on this device.")
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)

        var recognizedText: String? = null
        if (requestCode == REQUEST_CODE_SPEECH && resultCode == RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?: (data.extras?.get(RecognizerIntent.EXTRA_RESULTS) as? ArrayList<*>)?.filterIsInstance<String>()
            recognizedText = results?.firstOrNull { it.isNotBlank() }?.trim()
                ?: data.getStringExtra("android.speech.extra.TEXT")?.trim()
        }

        // Finish activity first so the OS starts window focus restoration to target app immediately
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)

        if (!recognizedText.isNullOrBlank()) {
            VoiceInputManager.onActivityRecognitionResult(recognizedText)
        } else {
            VoiceInputManager.onActivityDismissed()
        }
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun getLocaleTagForLanguage(language: String): String {
        return when (language.lowercase(Locale.ROOT)) {
            "bn" -> "bn-BD"
            "en" -> "en-US"
            "hi" -> "hi-IN"
            "ar" -> "ar-SA"
            "es" -> "es-ES"
            "fr" -> "fr-FR"
            else -> {
                try {
                    Locale.forLanguageTag(language).toLanguageTag().ifBlank {
                        Locale.getDefault().toLanguageTag()
                    }
                } catch (_: Exception) {
                    Locale.getDefault().toLanguageTag()
                }
            }
        }
    }

    companion object {
        const val EXTRA_LANGUAGE = "extra_language"
        private const val REQUEST_CODE_SPEECH = 4219
    }
}

