package com.deeni.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranWordAlignmentEngineTest {
    @Test fun returnsOriginalWordPositionsForMatchedWords() {
        val result = QuranWordAlignmentEngine.align(
            "بسم الله الرحمن الرحيم",
            "بسم الله الرحمن الرحيم"
        )
        assertEquals(4, result.matches.size)
        assertEquals(2, result.matches[2].expected.index)
        assertEquals(2, result.matches[2].transcript.index)
        assertFalse(result.requiresReview)
    }

    @Test fun identifiesOmittedAndAdditionalWordsWithIndexes() {
        val result = QuranWordAlignmentEngine.align(
            "الحمد لله رب العالمين",
            "الحمد لله العالمين زيادة"
        )
        assertEquals(listOf("رب"), result.missingExpected.map { it.text })
        assertEquals(2, result.missingExpected.single().index)
        assertEquals(listOf("زيادة"), result.extraTranscript.map { it.text })
        assertEquals(3, result.extraTranscript.single().index)
        assertTrue(result.requiresReview)
    }

    @Test fun normalizesTashkeelBeforeAlignment() {
        val result = QuranWordAlignmentEngine.align("الرَّحْمَٰنِ", "الرحمان")
        assertEquals(1, result.matches.size)
        assertFalse(result.requiresReview)
    }

    @Test fun handlesEmptyTranscript() {
        val result = QuranWordAlignmentEngine.align("بسم الله", "")
        assertEquals(0, result.matches.size)
        assertEquals(listOf(0, 1), result.missingExpected.map { it.index })
    }

    @Test fun handlesRepeatedWordsDeterministically() {
        val result = QuranWordAlignmentEngine.align("الله الله نور", "الله نور")
        assertEquals(listOf(0, 2), result.matches.map { it.expected.index })
        assertEquals(listOf(1), result.missingExpected.map { it.index })
    }
}
