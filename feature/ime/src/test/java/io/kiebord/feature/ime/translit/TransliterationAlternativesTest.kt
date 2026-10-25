package io.kiebord.feature.ime.translit

import io.kiebord.feature.ime.NativeEngine
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
    }
}
