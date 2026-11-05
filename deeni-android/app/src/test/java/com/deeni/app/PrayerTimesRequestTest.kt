package com.deeni.app

import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerTimesRequestTest {
    @Test
    fun buildsRequestWithSelectedCalculationMethod() {
        assertEquals(
            "https://api.aladhan.com/v1/timingsByCity?city=Abu+Arish&country=Saudi+Arabia&method=4",
            PrayerTimesRequest.url("Abu Arish", "Saudi Arabia", 4)
        )
        assertEquals(
            "https://api.aladhan.com/v1/timingsByCity?city=London&country=United+Kingdom&method=3",
            PrayerTimesRequest.url("London", "United Kingdom", 3)
        )
    }

    @Test
    fun encodesArabicAndReservedCharacters() {
        val url = PrayerTimesRequest.url("مكة المكرمة", "السعودية", 4)
        assert(url.contains("city=%D9%85%D9%83%D8%A9+%D8%A7%D9%84%D9%85%D9%83%D8%B1%D9%85%D8%A9"))
        assert(url.contains("country=%D8%A7%D9%84%D8%B3%D8%B9%D9%88%D8%AF%D9%8A%D8%A9"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankCity() {
        PrayerTimesRequest.url(" ", "Saudi Arabia", 4)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankCountry() {
        PrayerTimesRequest.url("Abu Arish", " ", 4)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsupportedCalculationMethod() {
        PrayerTimesRequest.url("Abu Arish", "Saudi Arabia", 999)
    }
}
