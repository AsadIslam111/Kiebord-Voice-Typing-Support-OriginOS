package io.kiebord.feature.ime.gesture

import androidx.compose.ui.geometry.Offset
import io.kiebord.feature.ime.NativeEngine
import io.kiebord.feature.ime.ui.suggestion.SuggestionCandidate
import kotlin.math.hypot

data class KeyCentroid(
    val character: String,
    val centerX: Float,
    val centerY: Float
)

class CrossLanguageGestureDecoder {

    /**
     * Decodes a raw touch trajectory against keyboard key centroids using native dictionary shape matching.
     * When typing in Borno Bengali mode with Latin keys, transliterates decoded candidates into Borno Bengali.
     */
    fun decodeGesture(
        points: List<Offset>,
        keyCentroids: List<KeyCentroid>,
        keyWidthPx: Float = 60f,
        lang: String = "en"
    ): List<SuggestionCandidate> {
        if (points.size < 2 || keyCentroids.isEmpty()) return emptyList()

        val decodedWords = if (NativeEngine.isAvailable()) {
            NativeEngine.queryGesture(points, keyCentroids, keyWidthPx, lang, 5)
        } else {
            emptyList()
        }

        if (decodedWords.isNotEmpty()) {
            val candidates = mutableListOf<SuggestionCandidate>()
            for (i in decodedWords.indices) {
                val word = decodedWords[i]
                candidates.add(
                    SuggestionCandidate(
                        text = word,
                        isPrimary = (i == 0),
                        rawInput = word
                    )
                )
            }
            return candidates
        }

        // Fallback: heuristic key sequence along trajectory
        val candidateChars = mutableListOf<String>()
        val startPoint = points.first()
        val endPoint = points.last()

        val startKey = keyCentroids.minByOrNull { hypot(it.centerX - startPoint.x, it.centerY - startPoint.y) }
        val endKey = keyCentroids.minByOrNull { hypot(it.centerX - endPoint.x, it.centerY - endPoint.y) }

        if (startKey != null && startKey.character.isNotBlank()) {
            candidateChars.add(startKey.character.lowercase())
        }

        val step = (points.size / 6).coerceAtLeast(1)
        for (i in step until points.size - step step step) {
            val pt = points[i]
            val closest = keyCentroids.minByOrNull { hypot(it.centerX - pt.x, it.centerY - pt.y) }
            if (closest != null && closest.character.isNotBlank()) {
                val ch = closest.character.lowercase()
                if (candidateChars.isEmpty() || candidateChars.last() != ch) {
                    candidateChars.add(ch)
                }
            }
        }

        if (endKey != null && endKey.character.isNotBlank()) {
            val endCh = endKey.character.lowercase()
            if (candidateChars.isEmpty() || candidateChars.last() != endCh) {
                candidateChars.add(endCh)
            }
        }

        val rawWord = candidateChars.joinToString("")
        if (rawWord.isBlank()) return emptyList()

        val isPhonetic = if (NativeEngine.isAvailable()) {
            NativeEngine.isPhoneticLayout(lang)
        } else {
            lang == "bn_borno"
        }

        if (isPhonetic) {
            val transliterated = if (NativeEngine.isAvailable()) {
                NativeEngine.transliterate(lang, rawWord)
            } else {
                rawWord
            }
            return listOf(
                SuggestionCandidate(
                    text = transliterated,
                    isPrimary = true,
                    rawInput = rawWord
                ),
                SuggestionCandidate(
                    text = rawWord,
                    isPrimary = false,
                    rawInput = rawWord
                )
            )
        } else {
            return listOf(
                SuggestionCandidate(
                    text = rawWord,
                    isPrimary = true,
                    rawInput = rawWord
                )
            )
        }
    }
}
