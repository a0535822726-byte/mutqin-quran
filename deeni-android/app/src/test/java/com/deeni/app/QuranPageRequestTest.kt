package com.deeni.app

import org.junit.Assert.assertEquals
import org.junit.Test

class QuranPageRequestTest {
    @Test
    fun buildsFirstAndLastMushafPageUrls() {
        assertEquals(
            "https://api.alquran.cloud/v1/page/1/quran-uthmani",
            QuranPageRequest.pageUrl(1)
        )
        assertEquals(
            "https://api.alquran.cloud/v1/page/604/quran-uthmani",
            QuranPageRequest.pageUrl(604)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPageZero() {
        QuranPageRequest.pageUrl(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPageBeyondMushaf() {
        QuranPageRequest.pageUrl(605)
    }
}
