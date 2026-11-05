package com.deeni.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranTextComparisonTest {
    @Test fun ignoresTashkeelAndAlifVariants() {
        val result = QuranTextComparison.compare("الرَّحْمَٰنِ", "الرحمان")
        assertFalse(result.hasDifferences)
        assertEquals(1, result.matchedWordCount)
    }

    @Test fun reportsOmittedAndAdditionalWordsAsTentativeTextDifferences() {
        val result = QuranTextComparison.compare("الحمد لله رب العالمين", "الحمد لله العالمين زيادة")
        assertEquals(listOf("رب"), result.missingFromTranscript)
        assertEquals(listOf("زيادة"), result.extraInTranscript)
    }

    @Test fun respectsWordOrder() {
        val result = QuranTextComparison.compare("رب العالمين", "العالمين رب")
        assertTrue(result.hasDifferences)
        assertEquals(1, result.matchedWordCount)
    }

    @Test fun handlesEmptyInput() {
        val result = QuranTextComparison.compare("بسم الله", "")
        assertEquals(listOf("بسم", "الله"), result.missingFromTranscript)
        assertEquals(0, result.matchedWordCount)
    }
}
