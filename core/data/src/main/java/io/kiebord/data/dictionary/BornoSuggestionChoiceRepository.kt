package io.kiebord.data.dictionary

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Serializable
internal data class BornoSuggestionChoicesPayload(
    val version: Int = 1,
    val choices: Map<String, String> = emptyMap()
)

/**
 * Repository that stores chosen suggestions for phonetic input sequences
 * in the Borno Phonetic layout.
 */
class BornoSuggestionChoiceRepository internal constructor(private val baseDir: File) {

    constructor(context: Context) : this(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val safe = if (context.isDeviceProtectedStorage) context else context.createDeviceProtectedStorageContext()
            safe.filesDir
        } else {
            context.filesDir
        }
    )

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var saveJob: Job? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val choices = ConcurrentHashMap<String, String>()
    private val reverseIndex = ConcurrentHashMap<String, MutableSet<String>>()

    private val storageFile: File by lazy {
        File(baseDir, "borno_suggestion_choices.json")
    }

    init {
        loadFromDisk()
    }

    /**
     * Records the chosen suggestion for a typed raw input sequence.
     */
    fun recordChoice(raw: String, word: String) {
        val cleanRaw = raw.trim()
        val cleanWord = word.trim()
        if (cleanRaw.length <= MIN_RAW_LENGTH || cleanWord.isEmpty() || cleanWord == cleanRaw) return

        val previousWord = choices.put(cleanRaw, cleanWord)
        if (previousWord != null && previousWord != cleanWord) {
            reverseIndex[previousWord]?.remove(cleanRaw)
            if (reverseIndex[previousWord]?.isEmpty() == true) {
                reverseIndex.remove(previousWord)
            }
        }

        reverseIndex.computeIfAbsent(cleanWord) { ConcurrentHashMap.newKeySet() }.add(cleanRaw)
        scheduleDebouncedSave()
    }

    /**
     * Retrieves the remembered suggestion for the typed [raw] input sequence.
     */
    fun getChoice(raw: String): String? {
        val cleanRaw = raw.trim()
        if (cleanRaw.length <= MIN_RAW_LENGTH) return null
        return choices[cleanRaw]
    }

    /**
     * Checks if a remembered suggestion exists for [raw].
     */
    fun hasChoice(raw: String): Boolean {
        return getChoice(raw) != null
    }

    /**
     * Checks if [word] is currently stored as a suggestion choice.
     */
    fun isChoiceWord(word: String): Boolean {
        val cleanWord = word.trim()
        return reverseIndex.containsKey(cleanWord)
    }

    /**
     * Unlearns all choices mapped to [word].
     */
    fun unlearnWord(word: String): Boolean {
        val cleanWord = word.trim()
        val rawSet = reverseIndex.remove(cleanWord) ?: return false
        var removed = false
        for (raw in rawSet) {
            if (choices.remove(raw) != null) {
                removed = true
            }
        }
        if (removed) {
            scheduleDebouncedSave()
        }
        return removed
    }

    /**
     * Unlearns the specific choice mapping for [raw].
     */
    fun unlearnChoice(raw: String): Boolean {
        val cleanRaw = raw.trim()
        val mappedWord = choices.remove(cleanRaw) ?: return false
        reverseIndex[mappedWord]?.remove(cleanRaw)
        if (reverseIndex[mappedWord]?.isEmpty() == true) {
            reverseIndex.remove(mappedWord)
        }
        scheduleDebouncedSave()
        return true
    }

    /**
     * Clears all remembered choices.
     */
    fun clearAll() {
        saveJob?.cancel()
        choices.clear()
        reverseIndex.clear()
        scope.launch {
            try {
                if (storageFile.exists()) {
                    storageFile.delete()
                }
            } catch (_: Throwable) {}
        }
    }

    private fun scheduleDebouncedSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(1500)
            saveToDisk()
        }
    }

    fun saveToDiskNow() {
        saveToDisk()
    }

    private fun saveToDisk() {
        try {
            val payload = BornoSuggestionChoicesPayload(choices = HashMap(choices))
            val jsonString = json.encodeToString(BornoSuggestionChoicesPayload.serializer(), payload)

            val tempFile = File(baseDir, "borno_suggestion_choices.json.tmp")
            tempFile.writeText(jsonString)
            if (tempFile.exists()) {
                if (storageFile.exists()) {
                    storageFile.delete()
                }
                if (!tempFile.renameTo(storageFile)) {
                    tempFile.copyTo(storageFile, overwrite = true)
                    tempFile.delete()
                }
            }
        } catch (_: Throwable) {}
    }

    private fun loadFromDisk() {
        try {
            if (!storageFile.exists()) return
            val content = storageFile.readText()
            if (content.isBlank()) return

            val payload = json.decodeFromString(BornoSuggestionChoicesPayload.serializer(), content)
            for ((raw, word) in payload.choices) {
                if (raw.length > MIN_RAW_LENGTH && word.isNotEmpty()) {
                    choices[raw] = word
                    reverseIndex.computeIfAbsent(word) { ConcurrentHashMap.newKeySet() }.add(raw)
                }
            }
        } catch (_: Throwable) {}
    }

    companion object {
        const val MIN_RAW_LENGTH = 1

        @Volatile
        private var instance: BornoSuggestionChoiceRepository? = null

        fun getInstance(context: Context): BornoSuggestionChoiceRepository {
            return instance ?: synchronized(this) {
                instance ?: BornoSuggestionChoiceRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
