package io.kiebord.feature.ime.action

import android.text.TextUtils

object CapsModeUtils {

    private fun isStartPunctuation(c: Char): Boolean {
        return c == '"' || c == '\'' || c == '“' || c == '‘' || c == '(' || c == '[' || c == '{' ||
                c == '¿' || c == '¡' || Character.getType(c) == Character.START_PUNCTUATION.toInt()
    }

    private fun isClosingPunctuation(c: Char): Boolean {
        return c == '"' || c == '\'' || c == '”' || c == '’' || c == ')' || c == ']' || c == '}' ||
                Character.getType(c) == Character.END_PUNCTUATION.toInt()
    }

    private fun isSentenceTerminator(c: Char): Boolean {
        return c == '.' || c == '!' || c == '?' || c == '।' || c == '॥'
    }

    private fun isAbbreviationMarker(c: Char): Boolean {
        return c == '.'
    }

    /**
     * Determines what caps mode should be in effect at the current offset in the text,
     * inspecting the text immediately preceding the cursor.
     * Ported and adapted from Borno Keyboard CapsModeUtils.
     *
     * @param cs The text preceding the cursor.
     * @param reqModes Combination of TextUtils.CAP_MODE_CHARACTERS, TextUtils.CAP_MODE_WORDS,
     *                 and TextUtils.CAP_MODE_SENTENCES.
     * @return The active caps mode bits.
     */
    fun getCapsMode(cs: CharSequence, reqModes: Int): Int {
        if ((reqModes and (TextUtils.CAP_MODE_WORDS or TextUtils.CAP_MODE_SENTENCES)) == 0) {
            return TextUtils.CAP_MODE_CHARACTERS and reqModes
        }

        // Step 1: Skip opening punctuation just before the cursor
        var i = cs.length
        while (i > 0 && isStartPunctuation(cs[i - 1])) {
            i--
        }

        // Step 2: Search for the start of a paragraph / line
        var j = i
        while (j > 0) {
            val c = cs[j - 1]
            if (c != ' ' && c != '\t') break
            j--
        }
        if (j <= 0) {
            // Start of text
            return (TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS or TextUtils.CAP_MODE_SENTENCES) and reqModes
        }
        val prevChar = cs[j - 1]
        if (prevChar == '\n' || prevChar == '\r') {
            // Start of line
            return (TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS or TextUtils.CAP_MODE_SENTENCES) and reqModes
        }

        if (i == j) {
            // No whitespace before cursor -> neither WORDS nor SENTENCES mode should be on
            return TextUtils.CAP_MODE_CHARACTERS and reqModes
        }

        if ((reqModes and TextUtils.CAP_MODE_SENTENCES) == 0) {
            return (TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS) and reqModes
        }

        // Step 3: Search for sentence terminators.
        // Skip any closing quotes / brackets between the terminator and the space (e.g. "Hello." )
        while (j > 0 && isClosingPunctuation(cs[j - 1])) {
            j--
        }

        if (j <= 0) {
            return TextUtils.CAP_MODE_CHARACTERS and reqModes
        }

        val c = cs[j - 1]
        j--

        if (isSentenceTerminator(c) && !isAbbreviationMarker(c)) {
            // '!' or '?' or '।' or '॥' followed by whitespace
            return (TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS or TextUtils.CAP_MODE_SENTENCES) and reqModes
        }

        if (c != '.' || j <= 0) {
            return (TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS) and reqModes
        }

        // Period '.' state machine to detect abbreviations like "e.g." vs sentence end "...yes."
        val startState = 0
        val wordState = 1
        val periodState = 2
        val letterState = 3

        val caps = (TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS or TextUtils.CAP_MODE_SENTENCES) and reqModes
        val noCaps = (TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS) and reqModes

        var state = startState
        while (j > 0) {
            val ch = cs[--j]
            when (state) {
                startState -> {
                    if (ch.isLetter()) {
                        state = wordState
                    } else if (ch.isWhitespace()) {
                        return noCaps
                    } else {
                        return caps
                    }
                }
                wordState -> {
                    if (ch.isLetter()) {
                        state = wordState
                    } else if (ch == '.') {
                        state = periodState
                    } else {
                        return caps
                    }
                }
                periodState -> {
                    if (ch.isLetter()) {
                        state = letterState
                    } else {
                        return caps
                    }
                }
                letterState -> {
                    if (ch.isLetter()) {
                        state = letterState
                    } else if (ch == '.') {
                        state = periodState
                    } else {
                        return noCaps
                    }
                }
            }
        }

        return if (state == startState || state == letterState) noCaps else caps
    }
}

