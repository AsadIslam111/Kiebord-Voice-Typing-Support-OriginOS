package com.codepotro.borno.feature.ime.translit

import com.codepotro.borno.feature.ime.NativeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneticAutoCorrectTest {

    @Test
    fun testDictionarySize() {
        assertTrue("Dictionary should contain over 2300 entries", PhoneticAutoCorrect.size() >= 2390)
    }

    @Test
    fun testCommonPhoneticCorrections() {
        assertEquals("bolole", PhoneticAutoCorrect.lookup("bolle"))
        assertEquals("amora", PhoneticAutoCorrect.lookup("amra"))
        assertEquals("aponi", PhoneticAutoCorrect.lookup("apni"))
        assertEquals("aponar", PhoneticAutoCorrect.lookup("apnar"))
        assertEquals("ebong", PhoneticAutoCorrect.lookup("abong"))
        assertEquals("fesbuk", PhoneticAutoCorrect.lookup("facebook"))
        assertEquals("iuTiub", PhoneticAutoCorrect.lookup("youtube"))
        assertEquals("allah্", PhoneticAutoCorrect.lookup("allah"))
    }

    @Test
    fun testCapitalizedInputFallback() {
        // When typing at start of sentence (e.g. "Amra" -> "Amora", "Apni" -> "Aponi")
        assertEquals("Amora", PhoneticAutoCorrect.lookup("Amra"))
        assertEquals("Aponi", PhoneticAutoCorrect.lookup("Apni"))
        assertEquals("Aponar", PhoneticAutoCorrect.lookup("Apnar"))
    }

    @Test
    fun testNonExistentWordReturnsNull() {
        assertNull(PhoneticAutoCorrect.lookup("xyzabc123"))
        assertEquals("xyzabc123", PhoneticAutoCorrect.correctOrSelf("xyzabc123"))
    }

    @Test
    fun testBornoPhoneticTransliterationWithAutoCorrect() {
        if (!NativeEngine.isAvailable()) return

        // Without auto-correct: "bolle" produces "বল্লে"
        val uncorrected = NativeEngine.transliterate("bn_borno", "bolle")
        assertEquals("বল্লে", uncorrected)

        // With auto-correct: "bolle" -> "bolole" produces "বললে"
        val correctedPhonetic = PhoneticAutoCorrect.lookup("bolle") ?: "bolle"
        assertEquals("bolole", correctedPhonetic)
        val correctedTranslit = NativeEngine.transliterate("bn_borno", correctedPhonetic)
        assertEquals("বললে", correctedTranslit)

        // "amra" -> "amora" produces "আমরা" instead of "আম্রা"
        assertEquals("আম্রা", NativeEngine.transliterate("bn_borno", "amra"))
        val amraCorrected = PhoneticAutoCorrect.correctOrSelf("amra")
        assertEquals("আমরা", NativeEngine.transliterate("bn_borno", amraCorrected))

        // "apni" -> "aponi" produces "আপনি" instead of "আপ্নি"
        assertEquals("আপ্নি", NativeEngine.transliterate("bn_borno", "apni"))
        val apniCorrected = PhoneticAutoCorrect.correctOrSelf("apni")
        assertEquals("আপনি", NativeEngine.transliterate("bn_borno", apniCorrected))
    }

    @Test
    fun testAvroPhoneticTransliterationWithAutoCorrect() {
        if (!NativeEngine.isAvailable()) return

        // Without auto-correct: "bolle" produces "বল্লে"
        val uncorrected = NativeEngine.transliterate("bn_avro", "bolle")
        assertEquals("বল্লে", uncorrected)

        // With auto-correct: "bolle" -> "bolole" produces "বললে"
        val correctedPhonetic = PhoneticAutoCorrect.lookup("bolle") ?: "bolle"
        val correctedTranslit = NativeEngine.transliterate("bn_avro", correctedPhonetic)
        assertEquals("বললে", correctedTranslit)

        // "amra" -> "amora" produces "আমরা"
        assertEquals("আম্রা", NativeEngine.transliterate("bn_avro", "amra"))
        val amraCorrected = PhoneticAutoCorrect.correctOrSelf("amra")
        assertEquals("আমরা", NativeEngine.transliterate("bn_avro", amraCorrected))

        // "apni" -> "aponi" produces "আপনি"
        assertEquals("আপ্নি", NativeEngine.transliterate("bn_avro", "apni"))
        val apniCorrected = PhoneticAutoCorrect.correctOrSelf("apni")
        assertEquals("আপনি", NativeEngine.transliterate("bn_avro", apniCorrected))
    }

    @Test
    fun testLookupPerformanceBenchmark() {
        val testWords = listOf("bolle", "amra", "apni", "facebook", "youtube", "not_in_dict", "hello", "advanced", "kolkata", "dhaka")
        val iterations = 50_000
        val start = System.nanoTime()
        var hits = 0
        for (i in 0 until iterations) {
            val word = testWords[i % testWords.size]
            if (PhoneticAutoCorrect.lookup(word) != null) {
                hits++
            }
        }
        val elapsedNs = System.nanoTime() - start
        val nsPerLookup = elapsedNs / iterations.toDouble()
        assertTrue("Lookup must be ultra-fast (< 500ns per lookup), was ${nsPerLookup}ns", nsPerLookup < 500)
    }
}