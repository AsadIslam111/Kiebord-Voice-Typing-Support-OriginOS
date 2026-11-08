package com.codepotro.borno.feature.ime.translit

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * BSTI engine that implements Bengali National typing rules:
 * - Traditional mode: pre-kar reordering, dual-kar composition, pre-kar conjunct continuation,
 *   ZWSP parking, phala-after-kar reordering, chandrabindu reordering, and reph reordering.
 * - Modern mode: standard direct Unicode entry while preserving Hasanta + Kar -> Independent Vowels,
 *   double-hasanta -> standalone hasanta, and অ + া -> আ.
 *   derived from Borno Keyboard BSTI Engine by Jayed Ahsan.
 */
object BengaliBstiEngine {

    const val BENGALI_HASANTA = 0x09CD // ্
    const val BENGALI_NUKTA = 0x09BC   // ়
    const val BENGALI_RA = 0x09B0      // র
    const val BENGALI_YA = 0x09AF      // য
    const val BENGALI_YAA = 0x09DF     // য়
    const val BENGALI_KHANDA_TA = 0x09CE // ৎ
    const val BENGALI_CHANDRABINDU = 0x0981 // ঁ
    const val BENGALI_A = 0x0985        // অ
    const val BENGALI_AA = 0x0986       // আ
    const val BENGALI_ZWNJ = 0x200C     // ZWNJ
    const val BENGALI_ZWSP = 0x200B     // ZWSP

    const val BENGALI_KAR_AA = 0x09BE  // া
    const val BENGALI_KAR_I = 0x09BF   // ি
    const val BENGALI_KAR_II = 0x09C0  // ী
    const val BENGALI_KAR_U = 0x09C1   // ু
    const val BENGALI_KAR_UU = 0x09C2  // ূ
    const val BENGALI_KAR_RRI = 0x09C3 // ৃ
    const val BENGALI_KAR_E = 0x09C7   // ে
    const val BENGALI_KAR_AI = 0x09C8  // ৈ
    const val BENGALI_KAR_O = 0x09CB   // ো
    const val BENGALI_KAR_AU = 0x09CC  // ৌ
    const val BENGALI_AU_LENGTH_MARK = 0x09D7 // ৗ

    // Precompiled regex patterns for BSTI / Bijoy typing rules
    private const val C = "(?:\\u09AC\\u09BC|[\\u0995-\\u09B9\\u09DC-\\u09DF\\u09CE])" // Consonants (including ব়)
    private const val CLUSTER = "(?:$C(?:\\u09CD$C)*+)" // Consonant cluster
    private const val ALL_KARS = "[\\u09BE-\\u09CC\\u09D7]" // All kars
    private const val PRE_KARS = "[\\u09BF\\u09C7\\u09C8\\u09CB\\u09CC]" // Pre-kars

    private val PATTERN_RA_PRE_KAR_JA_PHALA = Pattern.compile("\\u09B0\\u09CD($PRE_KARS)\\u09AF$")
    private val PATTERN_CHANDRABINDU_PHALA = Pattern.compile("(\\u0981)(\\u09CD[\\u09B0\\u09AF]|\\u09BC?\\u09CD[\\u09B0\\u09AF])$")
    private val PATTERN_CHANDRABINDU_KAR = Pattern.compile("(\\u0981)($ALL_KARS)$")
    private val PATTERN_DUAL_KAR_O = Pattern.compile("($CLUSTER)\\u09C7\\u09BE$")
    // ৌ (09CC) is a dedicated key in BSTI — only the Au Length Mark ৗ (09D7) completes the dual-kar
    private val PATTERN_DUAL_KAR_AU = Pattern.compile("($CLUSTER)\\u09C7\\u09D7$")
    private val PATTERN_REPH = Pattern.compile("($CLUSTER$ALL_KARS*+\\u0981*+)\\u09B0\\u09CD$")
    private val PATTERN_PHALA_AFTER_KAR = Pattern.compile("($ALL_KARS)(\\u09CD[\\u09B0\\u09AF]|\\u09BC?\\u09CD[\\u09B0\\u09AF])$")
    private val PATTERN_DUAL_KAR_AI = Pattern.compile("($CLUSTER)\\u09C7\\u09BF$")
    private val PATTERN_PRE_KAR_CONJUNCT = Pattern.compile("($CLUSTER)($PRE_KARS)(\\u09CD$C)$")
    private val PATTERN_PRE_KAR_SINGLE = Pattern.compile("(^|[^\\u0995-\\u09B9\\u09DC-\\u09DF\\u09CE])($PRE_KARS)($CLUSTER)$")
    private val PATTERN_INSERT_ZWSP = Pattern.compile("($CLUSTER)($PRE_KARS)$")
    // Fires when a new pre-kar is typed after a cluster that already has a kar attached
    // e.g. কে + ি → কে​ি (parks ি for the next consonant, preventing dual-kar collapse)
    private val PATTERN_INSERT_ZWSP_AFTER_KAR = Pattern.compile("($CLUSTER$ALL_KARS++)($PRE_KARS)$")
    private val PATTERN_PRE_KAR_WITH_ZWSP = Pattern.compile("\\u200B($PRE_KARS)($CLUSTER)$")
    private val PATTERN_CLEANUP_ZWSP = Pattern.compile("\\u200B(?![$PRE_KARS]$)")

    fun isBengaliKar(codePoint: Int): Boolean {
        return (codePoint in BENGALI_KAR_AA..BENGALI_KAR_AU) || codePoint == BENGALI_AU_LENGTH_MARK
    }

    fun isBengaliPreKar(codePoint: Int): Boolean {
        return codePoint == BENGALI_KAR_I ||  // ি
               codePoint == BENGALI_KAR_E ||  // ে
               codePoint == BENGALI_KAR_AI || // ৈ
               codePoint == BENGALI_KAR_O ||  // ো
               codePoint == BENGALI_KAR_AU    // ৌ
    }

    fun isBengaliConsonant(codePoint: Int): Boolean {
        return (codePoint in 0x0995..0x09A8)
                || (codePoint in 0x09AA..0x09B0)
                || codePoint == 0x09B2
                || (codePoint in 0x09B6..0x09B9)
                || codePoint == 0x09DC
                || codePoint == 0x09DD
                || codePoint == 0x09DF
                || codePoint == BENGALI_KHANDA_TA
    }

    fun getBengaliVowelForKar(karCodePoint: Int): Int {
        return when (karCodePoint) {
            BENGALI_KAR_AA -> 0x0986  // া -> আ
            BENGALI_KAR_I -> 0x0987   // ি -> ই
            BENGALI_KAR_II -> 0x0988  // ী -> ঈ
            BENGALI_KAR_U -> 0x0989   // ু -> উ
            BENGALI_KAR_UU -> 0x098A  // ূ -> ঊ
            BENGALI_KAR_RRI -> 0x098B // ৃ -> ঋ
            BENGALI_KAR_E -> 0x098F   // ে -> এ
            BENGALI_KAR_AI -> 0x0990  // ৈ -> ঐ
            BENGALI_KAR_O -> 0x0993   // ো -> ও
            BENGALI_KAR_AU -> 0x0994  // ৌ -> ঔ
            BENGALI_AU_LENGTH_MARK -> 0x0994 // ৗ -> ঔ
            else -> karCodePoint
        }
    }

    /**
     * Applies Bijoy / BSTI typing rules with configurable Traditional Mode.
     * Hasanta + Kar -> Independent Vowels applies to both modes.
     * Pre-kar and sequence reordering apply only in Traditional Mode.
     *
     * @param text The current composing text after key entry.
     * @param primaryCode The codePoint of the most recent key event.
     * @param isTraditionalMode If true, applies pre-kar and Bijoy sequence reordering.
     * @param isFromMacro If true, indicates multi-character entry such as a Phala key.
     * @return The transformed Unicode text.
     */
    fun applyBstiRules(
        text: String,
        primaryCode: Int,
        isTraditionalMode: Boolean = false,
        isFromMacro: Boolean = false
    ): String {
        if (text.length < 2) {
            return text
        }

        // 1. Hasanta + Kar -> Independent Vowel (Both Traditional and Modern)
        if (isBengaliKar(primaryCode)) {
            val len = text.length
            val karLen = Character.charCount(primaryCode)
            if (len >= karLen + 1) {
                val prevCode = text.codePointBefore(len - karLen)
                if (prevCode == BENGALI_HASANTA) {
                    val vowel = getBengaliVowelForKar(primaryCode)
                    return text.substring(0, len - karLen - 1) + String(Character.toChars(vowel))
                }
            }
        }

        // 1b. Hasanta + Au Length Mark (্ + ৗ) -> ঔ (Both modes)
        if (text.endsWith("\u09CD\u09D7")) {
            return text.substring(0, text.length - 2) + "\u0994"
        }

        // 2. gg (্ + ্) -> ্‌ (Standalone Hasanta U+09CD U+200C) (Both modes)
        if (text.endsWith("\u09CD\u09CD")) {
            return text.substring(0, text.length - 2) + "\u09CD\u200C"
        }

        // 3. Ff (অ + া) -> আ (Both modes)
        if (text.endsWith("\u0985\u09BE")) {
            return text.substring(0, text.length - 2) + "\u0986"
        }

        // 4. Ra + Ja-Phala (র + ্য) -> ব়্য (Both modes to prevent rendering as Reph র্য)
        if (primaryCode != BENGALI_YA || isFromMacro) {
            if (text.endsWith("\u09B0\u09CD\u09AF")) {
                return text.substring(0, text.length - 3) + "\u09AC\u09BC\u09CD\u09AF"
            }
            if (text.endsWith("\u09B0\u0981\u09CD\u09AF")) {
                return text.substring(0, text.length - 4) + "\u09AC\u09BC\u09CD\u09AF\u0981"
            }
        }

        // If not in Traditional Mode, skip pre-kar, dual-kar, reph, phala reordering
        if (!isTraditionalMode) {
            return text
        }

        var result = text
        var m: Matcher

        if (isBengaliPreKar(primaryCode)) {
            m = PATTERN_INSERT_ZWSP.matcher(result)
            if (m.find()) {
                result = m.replaceFirst("$1\u200B$2")
            } else {
                // New pre-kar typed after a cluster+kar block (e.g. কে + ি):
                // park it with ZWSP so dual-kar rules cannot collapse it with the existing kar.
                m = PATTERN_INSERT_ZWSP_AFTER_KAR.matcher(result)
                if (m.find()) {
                    result = m.replaceFirst("$1\u200B$2")
                }
            }
        }

        // 5. Ra + Pre-Kar + Ja-Phala (র্ + [ে/ি/ৈ/ো/ৌ] + য) -> ব়্য + Pre-Kar (e.g. ব়্যে)
        if (primaryCode != BENGALI_YA || isFromMacro) {
            m = PATTERN_RA_PRE_KAR_JA_PHALA.matcher(result)
            if (m.find()) {
                result = m.replaceFirst("\u09AC\u09BC\u09CD\u09AF$1")
            }
        }

        // 6. Chandrabindu fix (ঁ + ্য -> ্য + ঁ, ঁ + া -> া + ঁ)
        m = PATTERN_CHANDRABINDU_PHALA.matcher(result)
        if (m.find()) {
            result = m.replaceFirst("$2$1")
        }
        m = PATTERN_CHANDRABINDU_KAR.matcher(result)
        if (m.find()) {
            result = m.replaceFirst("$2$1")
        }

        // 7. Dual Kar (ে + া -> ো, ে + ৌ/ৗ -> ৌ)
        m = PATTERN_DUAL_KAR_O.matcher(result)
        if (m.find()) {
            result = m.replaceFirst("$1\u09CB")
        }
        m = PATTERN_DUAL_KAR_AU.matcher(result)
        if (m.find()) {
            result = m.replaceFirst("$1\u09CC")
        }
        m = PATTERN_DUAL_KAR_AI.matcher(result)
        if (m.find()) {
            result = m.replaceFirst("$1\u09C8")
        }

        // 9. Ra/Ja Phala typed after Kar -> move Phala before Kar
        m = PATTERN_PHALA_AFTER_KAR.matcher(result)
        if (m.find()) {
            result = m.replaceFirst("$2$1")
        }

        // 10. Pre-Kar conjunct continuation (ক + ি + ্ + ল -> ক + ্ল + ি)
        m = PATTERN_PRE_KAR_CONJUNCT.matcher(result)
        if (m.find()) {
            result = m.replaceFirst("$1$3$2")
        }

        // 12. Pre-Kar + Consonant/Cluster reordering (ি + ক -> ক + ি)
        m = PATTERN_PRE_KAR_SINGLE.matcher(result)
        if (m.find()) {
            result = m.replaceFirst("$1$3$2")
        }

        // 13. Pre-Kar (marked with ZWSP) jumping over newly typed cluster
        m = PATTERN_PRE_KAR_WITH_ZWSP.matcher(result)
        if (m.find()) {
            result = m.replaceFirst("$2$1")
        }

        // 14. Cleanup: Remove any ZWSP that is no longer actively waiting for the next keystroke.
        // A ZWSP is only active if it is immediately followed by a PRE_KAR at the very end of the string.
        m = PATTERN_CLEANUP_ZWSP.matcher(result)
        if (m.find()) {
            result = m.replaceAll("")
        }

        return result
    }

    /**
     * Reorders Reph (র + ্) to appear before the cluster.
     * This should be called explicitly when the Reph key is pressed in Traditional Mode.
     */
    fun applyRephReordering(text: String): String {
        if (text.length < 2) {
            return text
        }
        val m = PATTERN_REPH.matcher(text)
        if (m.find()) {
            return m.replaceFirst("\u09B0\u09CD$1")
        }
        return text
    }

    /**
     * Removes any pending zero-width space characters (e.g. When finishing composing).
     */
    fun cleanPending(text: String): String {
        return text.replace("\u200B", "")
    }
}
