package com.deeni.app

import org.junit.Assert.assertEquals
import org.junit.Test

class TafsirValidationTest {
    @Test
    fun acceptsMatchingVerseWithAllContent() {
        val result = TafsirValidation.result(
            requestedSurah = 1,
            requestedAyah = 1,
            returnedSurah = 1,
            returnedAyah = 1,
            surahName = "الفاتحة",
            ayahText = "بِسْمِ اللَّهِ",
            tafsirText = "شرح الآية"
        )
        assertEquals("الفاتحة", result.surahName)
        assertEquals(1, result.ayahNumber)
        assertEquals("بِسْمِ اللَّهِ", result.ayahText)
        assertEquals("شرح الآية", result.tafsirText)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsImpossibleRequestedAyah() {
        TafsirValidation.result(1, 8, 1, 8, "الفاتحة", "نص", "تفسير")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDifferentReturnedSurah() {
        TafsirValidation.result(2, 1, 3, 1, "آل عمران", "نص", "تفسير")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDifferentReturnedAyah() {
        TafsirValidation.result(2, 1, 2, 2, "البقرة", "نص", "تفسير")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingTafsirText() {
        TafsirValidation.result(1, 1, 1, 1, "الفاتحة", "نص", " ")
    }
}
