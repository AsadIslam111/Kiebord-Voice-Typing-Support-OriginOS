package com.codepotro.borno.feature.ime.voice

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.codepotro.borno.data.prefs.KiebordPreferences
import com.codepotro.borno.data.prefs.KiebordPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * State of the voice typing recognition lifecycle.
 */
sealed class VoiceInputState {
    data object Idle : VoiceInputState()
    data class Listening(val statusText: String = "Speak now", val rmsDb: Float = 0f) : VoiceInputState()
    data class Error(val message: String) : VoiceInputState()
}

/**
 * Manages speech recognition for Kiebord.
 *
 * Supports:
 * 1. Inline continuous dictation via SpeechRecognizer (for Global ROMs and devices supporting background IPC).
 * 2. Instant automatic fallback to Google Voice Popup Activity (ACTION_RECOGNIZE_SPEECH), which is
 *    100% immune to Chinese ROM (Vivo OriginOS, Xiaomi HyperOS, Oppo ColorOS) background service blocks.
 */
class VoiceInputManager(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val prefsRepo = KiebordPreferencesRepository.getInstance(context)
    private val voicePrefs by lazy { context.getSharedPreferences("kiebord_voice_prefs", Context.MODE_PRIVATE) }

    private var speechRecognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<VoiceInputState>(VoiceInputState.Idle)
    val state: StateFlow<VoiceInputState> = _state.asStateFlow()

    var onPartialResult: ((String) -> Unit)? = null
    var onFinalResult: ((String) -> Unit)? = null
    var onSpeechDismissed: (() -> Unit)? = null

    private var isListeningActive = false
    private var currentLanguage = "en"
    private var currentIntent: Intent? = null
    private var configuredProvider = KiebordPreferences.VOICE_PROVIDER_GOOGLE

    private var watchdogRunnable: Runnable? = null
    private var isLaunchingPopup = false

    init {
        activeManagerInstance = this

        coroutineScope.launch {
            prefsRepo.preferencesFlow.collect { prefs ->
                configuredProvider = prefs.voiceTypingProvider
            }
        }
    }

    val isListening: Boolean
        get() = isListeningActive

    /**
     * Resolves the BCP-47 locale tag matching the current keyboard layout.
     */
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

    /**
     * Discovers the best Google recognition service component available on this device.
     */
    private fun getBestRecognitionComponent(): ComponentName? {
        val pm = context.packageManager

        // 1. Check system secure setting (if user or ADB configured it)
        try {
            val systemService = Settings.Secure.getString(context.contentResolver, "voice_recognition_service")
            if (!systemService.isNullOrBlank()) {
                val cmp = ComponentName.unflattenFromString(systemService)
                if (cmp != null) {
                    val serviceInfo = pm.getServiceInfo(cmp, 0)
                    if (serviceInfo.isEnabled) return cmp
                }
            }
        } catch (_: Exception) {}

        val candidates = listOf(
            // Modern Google Speech Recognition & Synthesis package
            ComponentName(
                "com.google.android.tts",
                "com.google.android.apps.speech.tts.googletts.service.GoogleTTSRecognitionService"
            ),
            // Modern Google Search app package (official Android RecognitionService)
            ComponentName(
                "com.google.android.googlequicksearchbox",
                "com.google.android.voicesearch.serviceapi.GoogleRecognitionService"
            ),
            // Legacy Google Search app package
            ComponentName(
                "com.google.android.googlequicksearchbox",
                "com.google.android.voicesearch.service.RecognitionService"
            )
        )

        for (cmp in candidates) {
            try {
                val serviceInfo = pm.getServiceInfo(cmp, 0)
                if (serviceInfo.isEnabled) {
                    return cmp
                }
            } catch (_: Exception) {}
        }

        // Search for any recognition service registered under Google packages
        try {
            val intent = Intent("android.speech.RecognitionService")
            val services = pm.queryIntentServices(intent, 0)
            val googleService = services.firstOrNull {
                it.serviceInfo.packageName.startsWith("com.google.android")
            }
            if (googleService != null) {
                return ComponentName(googleService.serviceInfo.packageName, googleService.serviceInfo.name)
            }
        } catch (_: Exception) {}

        return null
    }

    private fun createSpeechRecognizer(): SpeechRecognizer {
        // 1. Android 12+ (API 31+): Check if on-device speech recognition is available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                    val onDevice = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                    if (onDevice != null) return onDevice
                }
            } catch (_: Exception) {}
        }

        // 2. Try explicit Google recognition service component
        val googleComponent = getBestRecognitionComponent()
        if (googleComponent != null) {
            try {
                return SpeechRecognizer.createSpeechRecognizer(context, googleComponent)
            } catch (_: Exception) {}
        }

        // 3. Fallback to system default speech recognizer (configured under Settings -> Assist & Voice input)
        return try {
            SpeechRecognizer.createSpeechRecognizer(context)
        } catch (_: Exception) {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
    }

    fun start(layoutLanguage: String) {
        currentLanguage = layoutLanguage

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            isPendingStart = true
            pendingLanguage = layoutLanguage
            val intent = Intent(context, VoicePermissionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            context.startActivity(intent)
            return
        }

        // Check if user or auto-detection prefers popup dialog
        val forcePopup = configuredProvider == KiebordPreferences.VOICE_PROVIDER_POPUP
        val forceInline = configuredProvider == KiebordPreferences.VOICE_PROVIDER_INLINE
        val autoFallbackActive = !forceInline && voicePrefs.getBoolean("auto_fallback_popup", false)

        if (forcePopup || autoFallbackActive) {
            launchVoicePopup(layoutLanguage)
            return
        }

        // Try silently configuring secure setting if permission is granted (e.g. Shizuku / system / ADB)
        try {
            val current = Settings.Secure.getString(context.contentResolver, "voice_recognition_service")
            if (current.isNullOrBlank()) {
                val googleComponent = getBestRecognitionComponent()
                if (googleComponent != null) {
                    Settings.Secure.putString(
                        context.contentResolver,
                        "voice_recognition_service",
                        googleComponent.flattenToString()
                    )
                }
            }
        } catch (_: SecurityException) {
            // Normal Android security sandbox behavior, ignored safely
        }

        // Otherwise try inline continuous dictation
        val isServiceAvailable = getBestRecognitionComponent() != null ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) ||
                SpeechRecognizer.isRecognitionAvailable(context)

        if (!isServiceAvailable && !forceInline) {
            // No background service queryable: fallback to popup immediately
            launchVoicePopup(layoutLanguage)
            return
        }

        isListeningActive = true
        _state.value = VoiceInputState.Listening("Speak now")

        val localeTag = getLocaleTagForLanguage(layoutLanguage)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
            putExtra("android.speech.extra.DICTATION_MODE", true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        currentIntent = intent

        startWatchdog(layoutLanguage)
        initAndStart(intent, layoutLanguage)
    }

    private fun startWatchdog(layoutLanguage: String) {
        cancelWatchdog()
        val isForceInline = configuredProvider == KiebordPreferences.VOICE_PROVIDER_INLINE
        val runnable = Runnable {
            if (isListeningActive && _state.value is VoiceInputState.Listening) {
                if (isForceInline) {
                    stop()
                    _state.value = VoiceInputState.Error("Service timeout (OriginOS blocked background binding).")
                    mainHandler.postDelayed({
                        if (_state.value is VoiceInputState.Error) {
                            _state.value = VoiceInputState.Idle
                        }
                    }, 4000)
                } else {
                    // Auto-fallback: learn that this device requires popup
                    voicePrefs.edit().putBoolean("auto_fallback_popup", true).apply()
                    launchVoicePopup(layoutLanguage)
                }
            }
        }
        watchdogRunnable = runnable
        // Allow up to 4000ms for Google's background recognition service to connect & prepare
        mainHandler.postDelayed(runnable, 4000)
    }

    private fun cancelWatchdog() {
        watchdogRunnable?.let { mainHandler.removeCallbacks(it) }
        watchdogRunnable = null
    }

    private fun initAndStart(intent: Intent, layoutLanguage: String) {
        val isForceInline = configuredProvider == KiebordPreferences.VOICE_PROVIDER_INLINE
        try {
            speechRecognizer?.destroy()
            speechRecognizer = createSpeechRecognizer().apply {
                setRecognitionListener(createListener(layoutLanguage))
                startListening(intent)
            }
        } catch (e: Exception) {
            cancelWatchdog()
            if (isForceInline) {
                stop()
                _state.value = VoiceInputState.Error("Init failed: ${e.message}")
                mainHandler.postDelayed({
                    if (_state.value is VoiceInputState.Error) {
                        _state.value = VoiceInputState.Idle
                    }
                }, 4000)
            } else {
                voicePrefs.edit().putBoolean("auto_fallback_popup", true).apply()
                launchVoicePopup(layoutLanguage)
            }
        }
    }

    private fun restartListening(recreate: Boolean = false) {
        if (!isListeningActive) return
        val intent = currentIntent ?: return
        mainHandler.post {
            if (!isListeningActive) return@post
            try {
                if (recreate || speechRecognizer == null) {
                    initAndStart(intent, currentLanguage)
                } else {
                    speechRecognizer?.cancel()
                    speechRecognizer?.startListening(intent)
                }
                _state.value = VoiceInputState.Listening("Speak now")
            } catch (_: Exception) {
                initAndStart(intent, currentLanguage)
            }
        }
    }

    private fun createListener(layoutLanguage: String): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                cancelWatchdog()
                if (isListeningActive) {
                    _state.value = VoiceInputState.Listening("Speak now")
                }
            }

            override fun onBeginningOfSpeech() {
                cancelWatchdog()
                if (isListeningActive) {
                    _state.value = VoiceInputState.Listening("Listening...")
                }
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (isListeningActive && _state.value is VoiceInputState.Listening) {
                    val currentText = (_state.value as VoiceInputState.Listening).statusText
                    _state.value = VoiceInputState.Listening(currentText, rmsdB)
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                if (isListeningActive) {
                    _state.value = VoiceInputState.Listening("Transcribing...")
                }
            }

            override fun onError(error: Int) {
                cancelWatchdog()
                val isForceInline = configuredProvider == KiebordPreferences.VOICE_PROVIDER_INLINE

                val errorDesc = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error (Error 3)"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error: ROM blocked background service (Error 5)"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied by ROM (Error 9)"
                    SpeechRecognizer.ERROR_NETWORK -> "Network connection error (Error 2)"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout (Error 1)"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected (Error 7)"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service busy (Error 8)"
                    SpeechRecognizer.ERROR_SERVER -> "Server error / language not supported (Error 4)"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout (Error 6)"
                    10 -> "Too many requests (Error 10)"
                    11 -> "Server disconnected (Error 11)"
                    12 -> "Language not supported by engine (Error 12)"
                    13 -> "Language pack missing on device (Error 13)"
                    else -> "Speech recognition error ($error)"
                }

                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        // User pause / silence in continuous listening: finalize any composing partial and restart smoothly
                        onSpeechDismissed?.invoke()
                        if (isListeningActive) {
                            restartListening()
                        }
                    }
                    SpeechRecognizer.ERROR_CLIENT,
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                    SpeechRecognizer.ERROR_SERVER -> {
                        if (isForceInline) {
                            stop()
                            _state.value = VoiceInputState.Error(errorDesc)
                            mainHandler.postDelayed({
                                if (_state.value is VoiceInputState.Error) {
                                    _state.value = VoiceInputState.Idle
                                }
                            }, 4000)
                        } else {
                            // Background binding blocked or service unavailable on this ROM: trigger popup fallback
                            voicePrefs.edit().putBoolean("auto_fallback_popup", true).apply()
                            launchVoicePopup(layoutLanguage)
                        }
                    }
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        stop()
                        _state.value = VoiceInputState.Error(errorDesc)
                        mainHandler.postDelayed({
                            if (_state.value is VoiceInputState.Error) {
                                _state.value = VoiceInputState.Idle
                            }
                        }, 4000)
                    }
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                        if (isListeningActive) {
                            _state.value = VoiceInputState.Listening("Reconnecting...")
                            mainHandler.postDelayed({
                                if (isListeningActive) restartListening()
                            }, 1200)
                        }
                    }
                    else -> {
                        if (isForceInline) {
                            stop()
                            _state.value = VoiceInputState.Error(errorDesc)
                            mainHandler.postDelayed({
                                if (_state.value is VoiceInputState.Error) {
                                    _state.value = VoiceInputState.Idle
                                }
                            }, 4000)
                        } else {
                            voicePrefs.edit().putBoolean("auto_fallback_popup", true).apply()
                            launchVoicePopup(layoutLanguage)
                        }
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                cancelWatchdog()
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim() ?: ""
                if (text.isNotBlank()) {
                    onFinalResult?.invoke(text)
                } else {
                    onSpeechDismissed?.invoke()
                }
                // Continuous listening: keep listening for next phrase as long as voice typing is active
                if (isListeningActive) {
                    restartListening()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                cancelWatchdog()
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank() && isListeningActive) {
                    _state.value = VoiceInputState.Listening("Listening...")
                    onPartialResult?.invoke(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    /**
     * Launches the foreground Google Voice Popup dialog Activity.
     * 100% immune to background service blocks on Vivo, Xiaomi, Oppo China ROMs.
     */
    fun launchVoicePopup(layoutLanguage: String) {
        cancelWatchdog()
        stop()
        if (isLaunchingPopup) return
        isLaunchingPopup = true
        mainHandler.post {
            try {
                val intent = Intent(context, VoiceRecognitionActivity::class.java).apply {
                    putExtra(VoiceRecognitionActivity.EXTRA_LANGUAGE, layoutLanguage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                isLaunchingPopup = false
                _state.value = VoiceInputState.Error("Voice dialog could not be opened: ${e.message}")
                mainHandler.postDelayed({
                    if (_state.value is VoiceInputState.Error) {
                        _state.value = VoiceInputState.Idle
                    }
                }, 2500)
            }
        }
    }

    fun stop() {
        cancelWatchdog()
        if (!isListeningActive && _state.value is VoiceInputState.Idle) return
        isListeningActive = false
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {}
            _state.value = VoiceInputState.Idle
            onSpeechDismissed?.invoke()
        }
    }

    fun destroy() {
        cancelWatchdog()
        stop()
        coroutineScope.cancel()
        if (activeManagerInstance === this) {
            activeManagerInstance = null
        }
    }

    companion object {
        @Volatile
        private var activeManagerInstance: VoiceInputManager? = null

        @Volatile
        var pendingStaticVoiceResult: String? = null
            private set

        @Synchronized
        fun consumePendingVoiceResult(): String? {
            val res = pendingStaticVoiceResult
            pendingStaticVoiceResult = null
            return res
        }

        private var isPendingStart = false
        private var pendingLanguage = "en"

        fun onPermissionResult(granted: Boolean) {
            if (granted && isPendingStart) {
                isPendingStart = false
                activeManagerInstance?.start(pendingLanguage)
            } else {
                isPendingStart = false
            }
        }

        fun onActivityRecognitionResult(text: String) {
            val trimmed = text.trim()
            activeManagerInstance?.isLaunchingPopup = false
            if (trimmed.isEmpty()) {
                onActivityDismissed()
                return
            }
            pendingStaticVoiceResult = trimmed
            activeManagerInstance?.mainHandler?.post {
                activeManagerInstance?._state?.value = VoiceInputState.Idle
                activeManagerInstance?.onFinalResult?.invoke(trimmed)
            }
        }

        fun onActivityDismissed() {
            activeManagerInstance?.isLaunchingPopup = false
            activeManagerInstance?.mainHandler?.post {
                activeManagerInstance?._state?.value = VoiceInputState.Idle
                activeManagerInstance?.onSpeechDismissed?.invoke()
            }
        }

        fun onActivityError(error: String) {
            activeManagerInstance?.isLaunchingPopup = false
            activeManagerInstance?.mainHandler?.post {
                activeManagerInstance?._state?.value = VoiceInputState.Error(error)
                activeManagerInstance?.mainHandler?.postDelayed({
                    if (activeManagerInstance?._state?.value is VoiceInputState.Error) {
                        activeManagerInstance?._state?.value = VoiceInputState.Idle
                    }
                }, 2500)
            }
        }

        fun resetPopupDetection(context: Context) {
            context.getSharedPreferences("kiebord_voice_prefs", Context.MODE_PRIVATE)
                .edit()
                .remove("auto_fallback_popup")
                .apply()
        }
    }
}
