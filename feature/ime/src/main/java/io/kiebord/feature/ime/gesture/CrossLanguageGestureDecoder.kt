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

        // Evaluate learned words directly against trajectory
        val matchingLearned = if (learnedWords.isNotEmpty()) {
            val keyMap = HashMap<Char, Offset>(keyCentroids.size)
            for (k in keyCentroids) {
                if (k.character.isNotEmpty()) {
                    keyMap[k.character[0].lowercaseChar()] = Offset(k.centerX, k.centerY)
                }
            }
            learnedWords.mapNotNull { lw ->
                val score = scoreLearnedWord(lw, points, keyMap, kw)
                if (score != null) lw to score else null
            }.sortedByDescending { it.second }.map { it.first }
        } else emptyList()

        val sortedWords = when {
            matchingLearned.isNotEmpty() -> {
                (matchingLearned + decodedWords).distinct()
            }
            learnedWords.isNotEmpty() -> {
                val (learnedMatches, otherMatches) = decodedWords.partition { it in learnedSet }
                (learnedMatches + otherMatches).distinct()
            }
            else -> {
                decodedWords
            }
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
        if (dStart > 2.2f || dEnd > 2.2f) return null

        var maxMiss = 0f
        var totalMiss = 0f
        var lastPointIdx = -1

        for (ch in chars) {
            val keyPos = keyMap[ch] ?: return null
            var minD = Float.MAX_VALUE
            var bestIdx = -1
            for (pIdx in points.indices) {
                val pt = points[pIdx]
                val d = hypot(pt.x - keyPos.x, pt.y - keyPos.y)
                if (d < minD) {
                    minD = d
                    bestIdx = pIdx
                }
            }
            val normD = minD / kw
            if (normD > 1.8f) return null
            if (normD > maxMiss) maxMiss = normD
            totalMiss += normD
            if (bestIdx < lastPointIdx - (points.size / 3)) return null
            if (bestIdx > lastPointIdx) lastPointIdx = bestIdx
        }

        val avgMiss = totalMiss / chars.size
        return 100f - (dStart * 15f + dEnd * 15f + avgMiss * 20f + maxMiss * 10f)
    }
}
