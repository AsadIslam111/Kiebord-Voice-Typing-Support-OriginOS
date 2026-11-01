package io.kiebord.feature.ime.translit

import io.kiebord.data.layout.PredefinedLayouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BornoEasyConjunctTest {

    data class TrailingClusterInfo(
        val cluster: String,
        val endsWithConsonant: Boolean,
        val endsWithHasanta: Boolean
    )

    private fun isBengaliConsonantChar(c: Char): Boolean {
        return (c in '\u0995'..'\u09B9') || (c in '\u09DC'..'\u09DF') || c == '\u09CE'
    }

    private fun extractTrailingBengaliCluster(text: String): TrailingClusterInfo {
        if (text.isEmpty()) return TrailingClusterInfo("", endsWithConsonant = false, endsWithHasanta = false)

        val lastChar = text.last()
        if (isBengaliConsonantChar(lastChar)) {
            var i = text.length - 1
            while (i >= 2) {
                val prevHasantaIdx = if (i >= 2 && text[i - 1] == '\u09BC') i - 2 else i - 1
                if (prevHasantaIdx >= 1 && text[prevHasantaIdx] == '\u09CD') {
                    val prevConsonantIdx = if (prevHasantaIdx >= 2 && text[prevHasantaIdx - 1] == '\u09BC') prevHasantaIdx - 2 else prevHasantaIdx - 1
                    if (isBengaliConsonantChar(text[prevConsonantIdx])) {
                        i = prevConsonantIdx
                    } else {
                        break
                    }
                } else {
                    break
                }
            }
            val cluster = text.substring(i)
            return TrailingClusterInfo(cluster, endsWithConsonant = true, endsWithHasanta = false)
        }

        if (lastChar == '\u09CD') {
            return TrailingClusterInfo("", endsWithConsonant = false, endsWithHasanta = true)
        }

        return TrailingClusterInfo("", endsWithConsonant = false, endsWithHasanta = false)
    }

    @Test
    fun testSingleConsonantExtraction() {
        val info = extractTrailingBengaliCluster("ক")
        assertTrue(info.endsWithConsonant)
        assertFalse(info.endsWithHasanta)
        assertEquals("ক", info.cluster)

        val infoRo = extractTrailingBengaliCluster("আমার")
        assertTrue(infoRo.endsWithConsonant)
        assertEquals("র", infoRo.cluster)
    }

    @Test
    fun testHasantaWaitingState() {
        val info = extractTrailingBengaliCluster("ক্")
        assertFalse(info.endsWithConsonant)
        assertTrue(info.endsWithHasanta)
        assertEquals("", info.cluster)

        val infoMid = extractTrailingBengaliCluster("যুক্ত্")
        assertFalse(infoMid.endsWithConsonant)
        assertTrue(infoMid.endsWithHasanta)
    }

    @Test
    fun testHasantaConjunctClusterExtraction() {
        // ক্ত (ক + ্ + ত)
        val infoKto = extractTrailingBengaliCluster("ক্ত")
        assertTrue(infoKto.endsWithConsonant)
        assertEquals("ক্ত", infoKto.cluster)

        // ক্ষ (ক + ্ + ষ)
        val infoKsho = extractTrailingBengaliCluster("ক্ষ")
        assertTrue(infoKsho.endsWithConsonant)
        assertEquals("ক্ষ", infoKsho.cluster)

        // জ্ঞ (জ + ্ + ঞ)
        val infoGyo = extractTrailingBengaliCluster("জ্ঞ")
        assertTrue(infoGyo.endsWithConsonant)
        assertEquals("জ্ঞ", infoGyo.cluster)

        // স্থ্য (স + ্ + থ + ্ + য)
        val infoSthyo = extractTrailingBengaliCluster("স্বাস্থ্য")
        assertTrue(infoSthyo.endsWithConsonant)
        assertEquals("স্থ্য", infoSthyo.cluster)

        // র্ক (র + ্ + ক)
        val infoRko = extractTrailingBengaliCluster("তর্ক")
        assertTrue(infoRko.endsWithConsonant)
        assertEquals("র্ক", infoRko.cluster)
    }

    @Test
    fun testWordsEndingInVowelOrKar() {
        // বাংলা ends in া -> not consonant
        val infoBangla = extractTrailingBengaliCluster("বাংলা")
        assertFalse(infoBangla.endsWithConsonant)
        assertFalse(infoBangla.endsWithHasanta)

        // শক্তি ends in ি -> not consonant
        val infoShokti = extractTrailingBengaliCluster("শক্তি")
        assertFalse(infoShokti.endsWithConsonant)
        assertFalse(infoShokti.endsWithHasanta)

        // বই ends in ই (independent vowel) -> not consonant
        val infoBoi = extractTrailingBengaliCluster("বই")
        assertFalse(infoBoi.endsWithConsonant)
        assertFalse(infoBoi.endsWithHasanta)
    }

    @Test
    fun testDynamicFirstRowKeysKo() {
        val row = PredefinedLayouts.getBornoEasyFirstRow(isKarMode = true, cluster = "ক")
        assertEquals(11, row.keys.size)

        // Key 0: cluster preview with inherent vowel confirmation
        val k0 = row.keys[0]
        assertEquals("ক", k0.label)
        assertEquals("", k0.commitText)

        // Keys 1-10: syllables with kars appended
        assertEquals("কা", row.keys[1].label)
        assertEquals("\u09BE", row.keys[1].commitText)

        assertEquals("কি", row.keys[2].label)
        assertEquals("\u09BF", row.keys[2].commitText)

        assertEquals("কী", row.keys[3].label)
        assertEquals("\u09C0", row.keys[3].commitText)

        assertEquals("কু", row.keys[4].label)
        assertEquals("\u09C1", row.keys[4].commitText)

        assertEquals("কূ", row.keys[5].label)
        assertEquals("\u09C2", row.keys[5].commitText)

        assertEquals("কৃ", row.keys[6].label)
        assertEquals("\u09C3", row.keys[6].commitText)

        assertEquals("কে", row.keys[7].label)
        assertEquals("\u09C7", row.keys[7].commitText)

        assertEquals("কৈ", row.keys[8].label)
        assertEquals("\u09C8", row.keys[8].commitText)

        assertEquals("কো", row.keys[9].label)
        assertEquals("\u09CB", row.keys[9].commitText)

        assertEquals("কৌ", row.keys[10].label)
        assertEquals("\u09CC", row.keys[10].commitText)
    }

    @Test
    fun testDynamicFirstRowKeysConjunctKto() {
        val row = PredefinedLayouts.getBornoEasyFirstRow(isKarMode = true, cluster = "ক্ত")
        assertEquals(11, row.keys.size)

        // Key 0 shows conjunct
        assertEquals("ক্ত", row.keys[0].label)
        assertEquals("", row.keys[0].commitText)

        // Keys 1-10 show conjunct with kars appended
        assertEquals("ক্তা", row.keys[1].label)
        assertEquals("\u09BE", row.keys[1].commitText)

        assertEquals("ক্তি", row.keys[2].label)
        assertEquals("\u09BF", row.keys[2].commitText)

        assertEquals("ক্তী", row.keys[3].label)
        assertEquals("\u09C0", row.keys[3].commitText)

        assertEquals("ক্তু", row.keys[4].label)
        assertEquals("\u09C1", row.keys[4].commitText)

        assertEquals("ক্তৌ", row.keys[10].label)
        assertEquals("\u09CC", row.keys[10].commitText)
    }

    @Test
    fun testVowelRowUnchangedWhenNotKarMode() {
        val row = PredefinedLayouts.getBornoEasyFirstRow(isKarMode = false, cluster = "ক্ত")
        assertEquals(11, row.keys.size)
        assertEquals("অ", row.keys[0].label)
        assertEquals("আ", row.keys[1].label)
        assertEquals("া", row.keys[1].flickUp)
        assertEquals("ঔ", row.keys[10].label)

        // Verify top row keys have no moreKeys popup
        for (key in row.keys) {
            assertTrue("Top row vowel keys should not have moreKeys", key.moreKeys.isEmpty())
        }
    }

    @Test
    fun testKarRowHasNoMoreKeys() {
        val karRow = PredefinedLayouts.getBornoEasyFirstRow(isKarMode = true, cluster = "ক")
        for (key in karRow.keys) {
            assertTrue("Top row kar keys should not have moreKeys", key.moreKeys.isEmpty())
        }
    }

    @Test
    fun testZwnjResetsCluster() {
        val info = extractTrailingBengaliCluster("ক‌")
        assertFalse(info.endsWithConsonant)
        assertFalse(info.endsWithHasanta)
        assertEquals("", info.cluster)
    }
}

