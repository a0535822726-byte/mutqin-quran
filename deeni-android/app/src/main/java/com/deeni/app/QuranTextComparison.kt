package com.deeni.app

/**
 * A conservative text-only comparison of a speech recognizer transcript with one selected ayah.
 * This is NOT Quran speech recognition and cannot judge pronunciation, tajweed, or whether
 * a difference was actually recited incorrectly. Speech-to-text engines can mishear Arabic.
 */
data class QuranTextDifference(
    val missingFromTranscript: List<String>,
    val extraInTranscript: List<String>,
    val matchedWordCount: Int,
    val expectedWordCount: Int
) {
    val hasDifferences: Boolean
        get() = missingFromTranscript.isNotEmpty() || extraInTranscript.isNotEmpty()
}

object QuranTextComparison {
    private val tashkeel = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")
    private val punctuation = Regex("[\\p{Punct}\\u060C\\u061B\\u061F\\u06D4]")

    fun normalize(text: String): List<String> {
        val cleaned = text
            .replace('ـ', ' ')
            .replace(tashkeel, "")
            .replace(punctuation, " ")
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
            .replace('ى', 'ي')
        return cleaned.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    }

    fun compare(expectedText: String, transcript: String): QuranTextDifference {
        val expected = normalize(expectedText)
        val actual = normalize(transcript)
        if (expected.isEmpty()) return QuranTextDifference(emptyList(), actual, 0, 0)
        if (actual.isEmpty()) return QuranTextDifference(expected, emptyList(), 0, expected.size)

        val rows = expected.size + 1
        val cols = actual.size + 1
        val lcs = Array(rows) { IntArray(cols) }
        for (i in expected.indices.reversed()) {
            for (j in actual.indices.reversed()) {
                lcs[i][j] = if (expected[i] == actual[j]) {
                    1 + lcs[i + 1][j + 1]
                } else {
                    maxOf(lcs[i + 1][j], lcs[i][j + 1])
                }
            }
        }

        val missing = mutableListOf<String>()
        val extra = mutableListOf<String>()
        var i = 0
        var j = 0
        while (i < expected.size && j < actual.size) {
            when {
                expected[i] == actual[j] -> { i++; j++ }
                lcs[i + 1][j] >= lcs[i][j + 1] -> missing.add(expected[i++])
                else -> extra.add(actual[j++])
            }
        }
        while (i < expected.size) missing.add(expected[i++])
        while (j < actual.size) extra.add(actual[j++])
        return QuranTextDifference(missing, extra, lcs[0][0], expected.size)
    }
}
