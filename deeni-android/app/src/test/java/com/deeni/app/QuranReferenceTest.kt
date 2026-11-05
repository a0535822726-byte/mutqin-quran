package com.deeni.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranReferenceTest {
    @Test
    fun standardMushafHas114SurahsAnd6236Ayahs() {
        assertEquals(114, QuranReference.surahCount())
        assertEquals(6236, QuranReference.totalAyahCount())
    }

    @Test
    fun hasKnownBoundaryCounts() {
        assertEquals(7, QuranReference.ayahCount(1))
        assertEquals(286, QuranReference.ayahCount(2))
        assertEquals(3, QuranReference.ayahCount(108))
        assertEquals(6, QuranReference.ayahCount(114))
    }

    @Test
    fun acceptsFirstAndLastAyahOfSurahs() {
        assertTrue(QuranReference.isValid(1, 1))
        assertTrue(QuranReference.isValid(1, 7))
        assertTrue(QuranReference.isValid(2, 286))
        assertTrue(QuranReference.isValid(114, 6))
    }

    @Test
    fun rejectsAyahBeyondSpecificSurahCount() {
        assertFalse(QuranReference.isValid(1, 8))
        assertFalse(QuranReference.isValid(108, 4))
        assertFalse(QuranReference.isValid(114, 7))
    }

    @Test
    fun rejectsInvalidSurahAndAyahNumbers() {
        assertFalse(QuranReference.isValid(0, 1))
        assertFalse(QuranReference.isValid(115, 1))
        assertFalse(QuranReference.isValid(1, 0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun requireValidRejectsImpossibleReference() {
        QuranReference.requireValid(1, 8)
    }
}
