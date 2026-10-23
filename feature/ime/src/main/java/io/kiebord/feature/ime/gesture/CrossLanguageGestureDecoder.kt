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
     * Scores a raw touch trajectory against keyboard key centroids using distance heuristics,
     * generates a primary Latin candidate, and transliterates into Borno Bengali candidate.
     */
    fun decodeGesture(
        points: List<Offset>,
        keyCentroids: List<KeyCentroid>
    ): List<SuggestionCandidate> {
        if (points.size < 2 || keyCentroids.isEmpty()) return emptyList()

        // 1. Sample key sequence along trajectory (start key, inflection points, end key)
        val candidateChars = mutableListOf<String>()
        val startPoint = points.first()
        val endPoint = points.last()

        val startKey = keyCentroids.minByOrNull { hypot(it.centerX - startPoint.x, it.centerY - startPoint.y) }
        val endKey = keyCentroids.minByOrNull { hypot(it.centerX - endPoint.x, it.centerY - endPoint.y) }

        if (startKey != null) {
            candidateChars.add(startKey.character.lowercase())
        }

        // Sample intermediate path points
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

        if (endKey != null) {
            val endCh = endKey.character.lowercase()
            if (candidateChars.isEmpty() || candidateChars.last() != endCh) {
                candidateChars.add(endCh)
            }
        }

        val latinWord = candidateChars.joinToString("")
        if (latinWord.isBlank()) return emptyList()

        // 2. Transliterate Latin sequence into Borno Bengali candidate
        val bengaliWord = if (NativeEngine.isAvailable()) {
            NativeEngine.transliterateBorno(latinWord)
        } else {
            latinWord
        }

        return listOf(
            SuggestionCandidate(
                text = bengaliWord,
                isPrimary = true,
                rawInput = latinWord
            ),
            SuggestionCandidate(
                text = latinWord,
                isPrimary = false,
                rawInput = latinWord
            )
        )
    }
}
