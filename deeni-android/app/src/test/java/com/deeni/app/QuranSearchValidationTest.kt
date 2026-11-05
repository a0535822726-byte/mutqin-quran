package com.deeni.app

import org.junit.Assert.assertEquals
import org.junit.Test

class QuranSearchValidationTest {
    @Test
    fun trimsSearchQuery() {
        assertEquals("الرحمن", QuranSearchValidation.query("  الرحمن  "))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankSearchQuery() {
        QuranSearchValidation.query("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsExcessivelyLongSearchQuery() {
        QuranSearchValidation.query("ا".repeat(121))
    }

    @Test
    fun acceptsValidSearchResult() {
        val result = QuranSearchValidation.result("الفاتحة", 1, 1, "بِسْمِ اللَّهِ")
        assertEquals(1, result.surahNumber)
        assertEquals(1, result.ayahNumber)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsImpossibleVerseInSearchResult() {
        QuranSearchValidation.result("الفاتحة", 1, 8, "نص")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptySearchResultText() {
        QuranSearchValidation.result("الفاتحة", 1, 1, " ")
    }
}
