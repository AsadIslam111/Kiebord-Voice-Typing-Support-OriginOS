package com.codepotro.borno.feature.ime.gesture

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossLanguageGestureDecoderTest {

    private val decoder = CrossLanguageGestureDecoder()

    private val qwertyCentroids = listOf(
        KeyCentroid("t", 180f, 50f),
        KeyCentroid("h", 240f, 120f),
        KeyCentroid("e", 100f, 50f),
        KeyCentroid("y", 220f, 50f),
        KeyCentroid("i", 300f, 50f),
        KeyCentroid("n", 260f, 190f)
    )

    @Test
    fun testEmptyOrTooShortPointsReturnsEmpty() {
        val result = decoder.decodeGesture(
            points = listOf(Offset(10f, 10f)),
            keyCentroids = qwertyCentroids,
            lang = "en"
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun testEmptyKeyCentroidsReturnsEmpty() {
        val result = decoder.decodeGesture(
            points = listOf(Offset(10f, 10f), Offset(20f, 20f)),
            keyCentroids = emptyList(),
            lang = "en"
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun testFallbackProducesCandidateForQwertyStroke() {
        val stroke = listOf(
            Offset(180f, 50f),  // t
            Offset(210f, 85f),
            Offset(240f, 120f), // h
            Offset(170f, 85f),
            Offset(100f, 50f)   // e
        )
        // Even when native engine is unavailable in JVM unit tests, fallback should succeed
        val results = decoder.decodeGesture(
            points = stroke,
            keyCentroids = qwertyCentroids,
            keyWidthPx = 40f,
            lang = "qwerty" // tests normalization of "qwerty" mode
        )
        assertFalse(results.isEmpty())
        val primary = results.first()
        assertTrue(primary.text.isNotEmpty())
    }

    @Test
    fun testLearnedWordScoringMatchesTrajectory() {
        val stroke = listOf(
            Offset(180f, 50f),  // t
            Offset(210f, 85f),
            Offset(240f, 120f), // h
            Offset(170f, 85f),
            Offset(100f, 50f)   // e
        )
        val learned = listOf("the", "unrelated")
        val results = decoder.decodeGesture(
            points = stroke,
            keyCentroids = qwertyCentroids,
            keyWidthPx = 40f,
            lang = "en",
            learnedWords = learned
        )
        assertFalse(results.isEmpty())
        assertEquals("the", results.first().text)
        assertTrue(results.first().isLearned)
    }

    @Test
    fun testLanguageFilterIsolatesBanglaAndEnglish() {
        val stroke = listOf(
            Offset(180f, 50f),
            Offset(240f, 120f),
            Offset(100f, 50f)
        )
        // With English language, Bangla script words should be ignored
        val results = decoder.decodeGesture(
            points = stroke,
            keyCentroids = qwertyCentroids,
            keyWidthPx = 40f,
            lang = "en",
            learnedWords = listOf("বাংলা", "the")
        )
        assertTrue(results.any { it.text == "the" })
        assertFalse(results.any { it.text == "বাংলা" })
    }
}

