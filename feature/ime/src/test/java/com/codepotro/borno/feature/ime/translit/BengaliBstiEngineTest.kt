package com.codepotro.borno.feature.ime.translit

import org.junit.Assert.assertEquals
import org.junit.Test

class BengaliBstiEngineTest {

    private fun simulateTyping(sequence: List<String>, isTraditional: Boolean = false): String {
        var current = ""
        for (charStr in sequence) {
            current += charStr
            if (charStr == "র্" && isTraditional) {
                current = BengaliBstiEngine.applyRephReordering(current)
            } else {
                var idx = 0
                val isMacro = charStr.length > 1
                while (idx < charStr.length) {
                    val cp = charStr.codePointAt(idx)
                    current = BengaliBstiEngine.applyBstiRules(current, cp, isTraditional, isMacro)
                    idx += Character.charCount(cp)
                }
            }
        }
        return BengaliBstiEngine.cleanPending(current)
    }

    @Test
    fun testHasantaPlusKarIndependentVowelsBothModes() {
        val pairs = listOf(
            listOf("্", "া") to "আ",
            listOf("্", "ি") to "ই",
            listOf("্", "ী") to "ঈ",
            listOf("্", "ু") to "উ",
            listOf("্", "ূ") to "ঊ",
            listOf("্", "ৃ") to "ঋ",
            listOf("্", "ে") to "এ",
            listOf("্", "ৈ") to "ঐ",
            listOf("্", "ো") to "ও",
            listOf("্", "ৌ") to "ঔ",
            listOf("্", "ৗ") to "ঔ"
        )

        for ((input, expected) in pairs) {
            assertEquals("Traditional: $input", expected, simulateTyping(input, isTraditional = true))
            assertEquals("Modern: $input", expected, simulateTyping(input, isTraditional = false))
        }
    }

    @Test
    fun testDoubleHasantaAndA() {
        assertEquals("্\u200C", simulateTyping(listOf("্", "্"), isTraditional = true))
        assertEquals("্\u200C", simulateTyping(listOf("্", "্"), isTraditional = false))

        assertEquals("আ", simulateTyping(listOf("অ", "া"), isTraditional = true))
        assertEquals("আ", simulateTyping(listOf("অ", "া"), isTraditional = false))
    }

    @Test
    fun testRaPlusJaPhala() {
        assertEquals("ব়্য", simulateTyping(listOf("র", "্য"), isTraditional = true))
        assertEquals("ব়্য", simulateTyping(listOf("র", "্য"), isTraditional = false))
    }

    @Test
    fun testTraditionalPreKarReordering() {
        // Typing 'ki': ি then ক -> কি
        assertEquals("কি", simulateTyping(listOf("ি", "ক"), isTraditional = true))
        // Typing 'ke': ে then ক -> কে
        assertEquals("কে", simulateTyping(listOf("ে", "ক"), isTraditional = true))
        // Typing 'koi': ৈ then ক -> কৈ
        assertEquals("কৈ", simulateTyping(listOf("ৈ", "ক"), isTraditional = true))

        // In Modern mode, typing ি then ক stays িক
        assertEquals("িক", simulateTyping(listOf("ি", "ক"), isTraditional = false))
    }

    @Test
    fun testDualKarCombination() {
        // Typing 'ko': ে then ক then া -> কো
        assertEquals("কো", simulateTyping(listOf("ে", "ক", "া"), isTraditional = true))
        // Typing 'kou': ে then ক then ৗ -> কৌ
        assertEquals("কৌ", simulateTyping(listOf("ে", "ক", "ৗ"), isTraditional = true))
        // Typing 'kai': ে then ক then ি -> কৈ
        assertEquals("কৈ", simulateTyping(listOf("ে", "ক", "ি"), isTraditional = true))
    }

    @Test
    fun testConjunctContinuationWithPreKar() {
        // ক + ি + ্ + ল -> ক্লি
        assertEquals("ক্লি", simulateTyping(listOf("ক", "ি", "্", "ল"), isTraditional = true))
    }

    @Test
    fun testParkedPreKarWithZwsp() {
        // Typing 'keli': কে + ি + ল -> কে + (parked ি) + ল -> কেলি
        assertEquals("কেলি", simulateTyping(listOf("ক", "ে", "ি", "ল"), isTraditional = true))
    }

    @Test
    fun testRephReordering() {
        // ক + র্ -> র্ক
        assertEquals("র্ক", simulateTyping(listOf("ক", "র্"), isTraditional = true))
        // কি + র্ -> র্কি
        assertEquals("র্কি", simulateTyping(listOf("ি", "ক", "র্"), isTraditional = true))
        // ভা + র্ -> র্ভা
        assertEquals("র্ভা", simulateTyping(listOf("ভ", "া", "র্"), isTraditional = true))
    }

    @Test
    fun testChandrabinduReordering() {
        // ঁ + া -> া + ঁ
        assertEquals("াঁ", simulateTyping(listOf("ঁ", "া"), isTraditional = true))
        // ঁ + ্য -> ্য + ঁ
        assertEquals("্যঁ", simulateTyping(listOf("ঁ", "্য"), isTraditional = true))
    }
}
