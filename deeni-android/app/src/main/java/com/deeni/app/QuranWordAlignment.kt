package com.deeni.app

/**
 * Word-position alignment for comparing a selected Quran passage with a speech-to-text
 * transcript. This is a text alignment utility only: it is not audio-to-word forced
 * alignment, tajweed assessment, or a definitive judgement that a recitation is wrong.
 */
internal data class QuranWordPosition(
    val index: Int,
    val text: String
)

internal data class QuranWordMatch(
    val expected: QuranWordPosition,
    val transcript: QuranWordPosition
)

internal data class QuranWordAlignment(
    val matches: List<QuranWordMatch>,
    val missingExpected: List<QuranWordPosition>,
    val extraTranscript: List<QuranWordPosition>,
    val expectedWordCount: Int,
    val transcriptWordCount: Int
) {
    /** Recognition differences must remain tentative because Arabic ASR can mishear recitation. */
    val requiresReview: Boolean
        get() = missingExpected.isNotEmpty() || extraTranscript.isNotEmpty()
}

/**
 * Produces stable word-index pairs using longest-common-subsequence alignment.
 * The indexes can later be connected to verified word geometry for visual highlighting.
 */
internal object QuranWordAlignmentEngine {
    fun align(expectedText: String, transcriptText: String): QuranWordAlignment {
        val expected = QuranTextComparison.normalize(expectedText)
            .mapIndexed { index, word -> QuranWordPosition(index, word) }
        val transcript = QuranTextComparison.normalize(transcriptText)
            .mapIndexed { index, word -> QuranWordPosition(index, word) }

        val dp = Array(expected.size + 1) { IntArray(transcript.size + 1) }
        for (i in expected.indices.reversed()) {
            for (j in transcript.indices.reversed()) {
                dp[i][j] = if (expected[i].text == transcript[j].text) {
                    1 + dp[i + 1][j + 1]
                } else {
                    maxOf(dp[i + 1][j], dp[i][j + 1])
                }
            }
        }

        val matches = mutableListOf<QuranWordMatch>()
        val missing = mutableListOf<QuranWordPosition>()
        val extra = mutableListOf<QuranWordPosition>()
        var i = 0
        var j = 0
        while (i < expected.size && j < transcript.size) {
            when {
                expected[i].text == transcript[j].text -> {
                    matches += QuranWordMatch(expected[i], transcript[j])
                    i++
                    j++
                }
                dp[i + 1][j] >= dp[i][j + 1] -> missing += expected[i++]
                else -> extra += transcript[j++]
            }
        }
        while (i < expected.size) missing += expected[i++]
        while (j < transcript.size) extra += transcript[j++]

        return QuranWordAlignment(
            matches = matches,
            missingExpected = missing,
            extraTranscript = extra,
            expectedWordCount = expected.size,
            transcriptWordCount = transcript.size
        )
    }
}
