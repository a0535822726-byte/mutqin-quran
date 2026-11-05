package com.deeni.app

import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerTimesValidationTest {
    @Test
    fun acceptsTwentyFourHourTimesAndRemovesTimezoneSuffix() {
        assertEquals("04:35", PrayerTimesValidation.time("04:35 (AST)", "Fajr"))
        assertEquals("23:59", PrayerTimesValidation.time("23:59", "Isha"))
        assertEquals("00:00", PrayerTimesValidation.time("00:00", "Fajr"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingTime() {
        PrayerTimesValidation.time("", "Fajr")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsImpossibleHour() {
        PrayerTimesValidation.time("24:10", "Fajr")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsImpossibleMinute() {
        PrayerTimesValidation.time("12:60", "Dhuhr")
    }

    @Test
    fun acceptsValidGeographicCoordinates() {
        assertEquals(16.97 to 42.83, PrayerTimesValidation.coordinates(16.97, 42.83))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsLatitudeOutsideEarth() {
        PrayerTimesValidation.coordinates(91.0, 42.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsLongitudeOutsideEarth() {
        PrayerTimesValidation.coordinates(16.0, 181.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonFiniteCoordinates() {
        PrayerTimesValidation.coordinates(Double.NaN, 42.0)
    }
}
