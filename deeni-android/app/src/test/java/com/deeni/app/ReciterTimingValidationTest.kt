package com.deeni.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ReciterTimingValidationTest {
    @Test
    fun sortsValidTimingsByStartTime() {
        val result = ReciterTimingValidation.validate(
            1,
            listOf(AyahTiming(2, 2500, 4000), AyahTiming(1, 0, 2400))
        )
        assertEquals(listOf(1, 2), result.map { it.ayah })
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAyahOutsideSurah() {
        ReciterTimingValidation.validate(1, listOf(AyahTiming(8, 0, 1000)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeStartTime() {
        ReciterTimingValidation.validate(1, listOf(AyahTiming(1, -1, 1000)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEndBeforeStart() {
        ReciterTimingValidation.validate(1, listOf(AyahTiming(1, 1000, 1000)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicateAyahTimings() {
        ReciterTimingValidation.validate(
            1,
            listOf(AyahTiming(1, 0, 1000), AyahTiming(1, 1200, 2000))
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyTimingList() {
        ReciterTimingValidation.validate(1, emptyList())
    }
}
