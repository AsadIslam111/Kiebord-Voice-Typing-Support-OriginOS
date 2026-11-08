package com.codepotro.borno.feature.ime.translit


object PhoneticAutoCorrect {
    private const val TABLE_SIZE = 8192
    private const val MASK = TABLE_SIZE - 1

    private val hashes = IntArray(TABLE_SIZE)
    private val keys = arrayOfNulls<String>(TABLE_SIZE)
    private val values = arrayOfNulls<String>(TABLE_SIZE)

    private var entryCount = 0

    init {
        loadData()
    }

    private fun loadData() {
        for (chunk in PhoneticAutoCorrectData.CHUNKS) {
            var start = 0
            val len = chunk.length
            while (start < len) {
                val eqIdx = chunk.indexOf('=', start)
                if (eqIdx < 0) break
                val semiIdx = chunk.indexOf(';', eqIdx)
                val end = if (semiIdx >= 0) semiIdx else len

                val key = chunk.substring(start, eqIdx)
                val value = chunk.substring(eqIdx + 1, end)
                insert(key, value)

                if (semiIdx < 0) break
                start = semiIdx + 1
            }
        }
    }

    private fun insert(key: String, value: String) {
        val h = key.hashCode()
        var idx = h and MASK
        while (keys[idx] != null && keys[idx] != key) {
            idx = (idx + 1) and MASK
        }
        if (keys[idx] == null) {
            entryCount++
        }
        hashes[idx] = h
        keys[idx] = key
        values[idx] = value
    }

    /**
     * Looks up phonetic correction for [raw] input in O(1) time with 0 allocations.
     * Returns null if no correction is found.
     */
    fun lookup(raw: String): String? {
        if (raw.isEmpty()) return null

        val h = raw.hashCode()
        var idx = h and MASK
        while (true) {
            val k = keys[idx] ?: break
            if (hashes[idx] == h && k == raw) {
                return values[idx]
            }
            idx = (idx + 1) and MASK
        }

        // Fallback for capitalized input (e.g. at start of sentence: "Amra" -> "amra" -> "amora" -> "Amora")
        if (raw[0].isUpperCase()) {
            val lowerFirst = raw.replaceFirstChar { it.lowercase() }
            val lowerH = lowerFirst.hashCode()
            var lowerIdx = lowerH and MASK
            while (true) {
                val k = keys[lowerIdx] ?: break
                if (hashes[lowerIdx] == lowerH && k == lowerFirst) {
                    val corrected = values[lowerIdx] ?: return null
                    return if (corrected.isNotEmpty() && corrected[0].isLowerCase()) {
                        corrected.replaceFirstChar { it.uppercase() }
                    } else {
                        corrected
                    }
                }
                lowerIdx = (lowerIdx + 1) and MASK
            }
        }

        return null
    }

    /**
     * Returns the corrected phonetic representation if found, or [raw] if no correction exists.
     */
    fun correctOrSelf(raw: String): String {
        return lookup(raw) ?: raw
    }

    /**
     * Returns total number of unique correction pairs loaded.
     */
    fun size(): Int = entryCount
}

