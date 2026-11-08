package com.codepotro.borno.feature.ime.action

import android.text.TextUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CapsModeUtilsTest {

    private val allModes = TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS or TextUtils.CAP_MODE_SENTENCES
    private val sentencesOnly = TextUtils.CAP_MODE_SENTENCES

    @Test
    fun testEmptyBufferYieldsSentenceCaps() {
        val mode = CapsModeUtils.getCapsMode("", sentencesOnly)
        assertEquals(TextUtils.CAP_MODE_SENTENCES, mode)
    }

    @Test
    fun testLeadingWhitespaceAndNewlinesYieldSentenceCaps() {
        val spaceOnly = CapsModeUtils.getCapsMode("   ", sentencesOnly)
        assertEquals(TextUtils.CAP_MODE_SENTENCES, spaceOnly)

        val newlineOnly = CapsModeUtils.getCapsMode("\n", sentencesOnly)
        assertEquals(TextUtils.CAP_MODE_SENTENCES, newlineOnly)

        val paragraphStart = CapsModeUtils.getCapsMode("First paragraph.\n\n", sentencesOnly)
        assertEquals(TextUtils.CAP_MODE_SENTENCES, paragraphStart)
    }

    @Test
    fun testMidWordDoesNotCapitalize() {
        // e.g. cursor at nor|mal
        val mode = CapsModeUtils.getCapsMode("this is a nor", sentencesOnly)
        assertEquals(0, mode)

        val modeAll = CapsModeUtils.getCapsMode("nor", allModes)
        // Mid-word without whitespace before cursor should only have CHARACTERS if requested
        assertEquals(TextUtils.CAP_MODE_CHARACTERS, modeAll)
    }

    @Test
    fun testWordFollowedBySpaceYieldsWordCapsNotSentenceCaps() {
        val mode = CapsModeUtils.getCapsMode("this is a ", allModes)
        assertTrue((mode and TextUtils.CAP_MODE_WORDS) != 0)
        assertEquals(0, mode and TextUtils.CAP_MODE_SENTENCES)
    }

    @Test
    fun testSentenceTerminatorsWithSpaceYieldSentenceCaps() {
        val period = CapsModeUtils.getCapsMode("Hello. ", sentencesOnly)
        assertEquals(TextUtils.CAP_MODE_SENTENCES, period)

        val question = CapsModeUtils.getCapsMode("How are you? ", sentencesOnly)
        assertEquals(TextUtils.CAP_MODE_SENTENCES, question)

        val exclamation = CapsModeUtils.getCapsMode("Great job! ", sentencesOnly)
        assertEquals(TextUtils.CAP_MODE_SENTENCES, exclamation)

        val banglaDari = CapsModeUtils.getCapsMode("আমি ভালো আছি। ", sentencesOnly)
        assertEquals(TextUtils.CAP_MODE_SENTENCES, banglaDari)

        val banglaDoubleDari = CapsModeUtils.getCapsMode("গল্পের শেষ॥ ", sentencesOnly)
        assertEquals(TextUtils.CAP_MODE_SENTENCES, banglaDoubleDari)
    }

    @Test
    fun testPeriodWithoutSpaceDoesNotYieldSentenceCaps() {
        val dot = CapsModeUtils.getCapsMode("Hello.", sentencesOnly)
        assertEquals(0, dot)

        val decimal = CapsModeUtils.getCapsMode("3.14", sentencesOnly)
        assertEquals(0, decimal)

        val script = CapsModeUtils.getCapsMode("./script", sentencesOnly)
        assertEquals(0, script)
    }

    @Test
    fun testAbbreviationDoesNotYieldSentenceCaps() {
        val eg = CapsModeUtils.getCapsMode("e.g. ", sentencesOnly)
        assertEquals(0, eg)
    }

    @Test
    fun testPunctuationWithQuotesAndBrackets() {
        val quotePeriod = CapsModeUtils.getCapsMode("\"Hello.\" ", sentencesOnly)
        assertEquals(TextUtils.CAP_MODE_SENTENCES, quotePeriod)

        val bracketExclamation = CapsModeUtils.getCapsMode("(Awesome!) ", sentencesOnly)
        assertEquals(TextUtils.CAP_MODE_SENTENCES, bracketExclamation)
    }
}

