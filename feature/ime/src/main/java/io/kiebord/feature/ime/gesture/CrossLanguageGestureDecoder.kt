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
     * Accurately matches and boosts personalized learned words.
     */
    fun decodeGesture(
        points: List<Offset>,
        keyCentroids: List<KeyCentroid>,
        keyWidthPx: Float = 60f,
        lang: String = "en",
        learnedWords: List<String> = emptyList()
    ): List<SuggestionCandidate> {
        if (points.size < 2 || keyCentroids.isEmpty()) return emptyList()

        val kw = keyWidthPx.coerceAtLeast(10f)
        val decodedWords = if (NativeEngine.isAvailable()) {
            NativeEngine.queryGesture(points, keyCentroids, kw, lang, 8, learnedWords)
        } else {
            emptyList()
        }

        val learnedSet = learnedWords.toSet()

        // Prioritize dictionary decoded words first!
        val sortedWords = if (learnedWords.isNotEmpty()) {
            val decodedSet = decodedWords.toSet()
            val matchingLearned = if (keyCentroids.isNotEmpty()) {
                val keyMap = HashMap<Char, Offset>(keyCentroids.size)
                for (k in keyCentroids) {
                    if (k.character.isNotEmpty()) {
                        keyMap[k.character[0].lowercaseChar()] = Offset(k.centerX, k.centerY)
                    }
                }
                learnedWords
                    .filter { !decodedSet.contains(it) }
                    .mapNotNull { lw ->
                        val score = scoreLearnedWord(lw, points, keyMap, kw)
                        if (score != null) lw to score else null
                    }
                    .sortedByDescending { it.second }
                    .map { it.first }
            } else emptyList()

            // Dictionary words first, then append strictly-matched learned words
            (decodedWords + matchingLearned).distinct().take(8)
        } else {
            decodedWords
        }

        if (sortedWords.isNotEmpty()) {
            val candidates = mutableListOf<SuggestionCandidate>()
            for (i in sortedWords.indices) {
                val word = sortedWords[i]
                candidates.add(
                    SuggestionCandidate(
                        text = word,
                        isPrimary = (i == 0),
                        rawInput = word,
                        isLearned = learnedSet.contains(word)
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

    private fun scoreLearnedWord(
        word: String,
        points: List<Offset>,
        keyMap: Map<Char, Offset>,
        kw: Float
    ): Float? {
        val clean = word.trim()
        if (clean.length < 2 || points.size < 2) return null
        val chars = clean.map { it.lowercaseChar() }
        val startKey = keyMap[chars.first()] ?: return null
        val endKey = keyMap[chars.last()] ?: return null

        val startPt = points.first()
        val endPt = points.last()

        val dStart = hypot(startPt.x - startKey.x, startPt.y - startKey.y) / kw
        val dEnd = hypot(endPt.x - endKey.x, endPt.y - endKey.y) / kw
        if (dStart > 1.15f || dEnd > 1.15f) return null

        var maxMiss = 0f
        var totalMiss = 0f
        var lastPointIdx = 0

        for (ch in chars) {
            val keyPos = keyMap[ch] ?: return null
            var minD = Float.MAX_VALUE
            var bestIdx = 0
            for (pIdx in points.indices) {
                val pt = points[pIdx]
                val d = hypot(pt.x - keyPos.x, pt.y - keyPos.y)
                if (d < minD) {
                    minD = d
                    bestIdx = pIdx
                }
            }
            val normD = minD / kw
            // All characters must be closely approached along trajectory
            if (normD > 0.95f) return null
            if (normD > maxMiss) maxMiss = normD
            totalMiss += normD

            // Sequential progression: gesture must progress forward through word characters
            if (bestIdx < (lastPointIdx - 1).coerceAtLeast(0)) return null
            if (bestIdx > lastPointIdx) lastPointIdx = bestIdx
        }

        val avgMiss = totalMiss / chars.size
        val cost = dStart * 0.5f + dEnd * 0.5f + avgMiss * 0.5f + maxMiss * 0.5f
        if (cost > 1.8f) return null
        return (100f * kotlin.math.exp(-cost * 1.5f)).coerceIn(0f, 100f)
    }
}
