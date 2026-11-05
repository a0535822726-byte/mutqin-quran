package com.deeni.app

import org.junit.Assert.assertEquals
import org.junit.Test

class QuranPageValidationTest {
    @Test
    fun acceptsFirstAndLastPage() {
        assertEquals(1, QuranPageValidation.pageNumber(1))
        assertEquals(604, QuranPageValidation.pageNumber(604))
    }

    @Test
    fun acceptsMatchingRequestedAndReturnedPage() {
        assertEquals(255, QuranPageValidation.pageNumber(255, 255))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPageOutsideMushaf() {
        QuranPageValidation.pageNumber(605)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsServiceReturningWrongPage() {
        QuranPageValidation.pageNumber(255, 256)
    }

    @Test
    fun acceptsValidNonEmptyAyahText() {
        assertEquals("بِسْمِ اللَّهِ", QuranPageValidation.ayahText(1, 1, "بِسْمِ اللَّهِ"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAyahNumberBeyondSurah() {
        QuranPageValidation.ayahText(1, 8, "نص")
    }

    @Test
    fun acceptsMatchingAyahIdentity() {
        assertEquals("نص", QuranPageValidation.ayahResponse(2, 255, 2, 255, "نص"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDifferentReturnedSurah() {
        QuranPageValidation.ayahResponse(2, 255, 3, 255, "نص")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDifferentReturnedAyah() {
        QuranPageValidation.ayahResponse(2, 255, 2, 256, "نص")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyAyahText() {
        QuranPageValidation.ayahText(1, 1, "  ")
    }
}
