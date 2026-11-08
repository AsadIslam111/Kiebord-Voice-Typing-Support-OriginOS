package com.codepotro.borno.feature.ime.translit

import com.codepotro.borno.feature.ime.NativeEngine
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransliterationAlternativesTest {

    @Test
    fun testEmptyInputReturnsEmptyList() {
        val results = NativeEngine.queryTransliterationAlternatives("bn_borno", "")
        assertTrue(results.isEmpty())
    }

    @Test
    fun testBlankInputReturnsEmptyList() {
        val results = NativeEngine.queryTransliterationAlternatives("bn_borno", "   ")
        assertTrue(results.isEmpty())
    }

    @Test
    fun testTransliterationAlternativesForPhoneticInputs() {
        val karonAlts = NativeEngine.queryTransliterationAlternatives("bn_borno", "karon")
        assertNotNull(karonAlts)
        assertTrue("Expected candidates for karon, got $karonAlts", karonAlts.isNotEmpty())

        val tumiAlts = NativeEngine.queryTransliterationAlternatives("bn_borno", "tumi")
        assertNotNull(tumiAlts)
        assertTrue("Expected candidates for tumi, got $tumiAlts", tumiAlts.isNotEmpty())

        val jaboAlts = NativeEngine.queryTransliterationAlternatives("bn_borno", "jabo")
        assertNotNull(jaboAlts)
        assertTrue("Expected candidates for jabo, got $jaboAlts", jaboAlts.isNotEmpty())

        val noukaAlts = NativeEngine.queryTransliterationAlternatives("bn_borno", "nouka")
        assertNotNull(noukaAlts)
        assertTrue("Expected candidates for nouka, got $noukaAlts", noukaAlts.isNotEmpty())

        val boikalAlts = NativeEngine.queryTransliterationAlternatives("bn_borno", "boikal")
        assertNotNull(boikalAlts)
        assertTrue("Expected candidates for boikal, got $boikalAlts", boikalAlts.isNotEmpty())

        val krishiAlts = NativeEngine.queryTransliterationAlternatives("bn_borno", "krishi")
        assertNotNull(krishiAlts)
        assertTrue("Expected candidates for krishi, got $krishiAlts", krishiAlts.isNotEmpty())
    }

    @Test
    fun testRawInputCandidateRepresentation() {
        val rawInput = "karon"
        val rawCandidate = com.codepotro.borno.feature.ime.ui.suggestion.SuggestionCandidate(
            text = rawInput,
            isPrimary = false,
            rawInput = rawInput
        )
        assertTrue(rawCandidate.rawInput != null && rawCandidate.text == rawCandidate.rawInput)

        val primaryCandidate = com.codepotro.borno.feature.ime.ui.suggestion.SuggestionCandidate(
            text = "কারণ",
            isPrimary = true,
            rawInput = rawInput
        )
        val list = listOf(rawCandidate, primaryCandidate)
        org.junit.Assert.assertEquals("karon", list[0].text)
        org.junit.Assert.assertEquals("কারণ", list[1].text)
        org.junit.Assert.assertFalse(list[0].isPrimary)
        assertTrue(list[1].isPrimary)
    }
}
