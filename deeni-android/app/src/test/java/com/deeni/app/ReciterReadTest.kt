package com.deeni.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ReciterReadTest {
    private val read = ReciterRead(
        reciterName = "قارئ تجريبي",
        readName = "حفص عن عاصم",
        readId = 7,
        audioServer = "https://audio.example.test/recitation/",
        surahIds = setOf(1, 2, 114),
        timingReadId = 12
    )

    @Test
    fun audioUrlUsesThreeDigitSurahNumberAndRemovesTrailingSlash() {
        assertEquals("https://audio.example.test/recitation/001.mp3", read.audioUrl(1))
        assertEquals("https://audio.example.test/recitation/114.mp3", read.audioUrl(114))
    }

    @Test(expected = IllegalArgumentException::class)
    fun audioUrlRejectsSurahNotProvidedByThisRead() {
        read.audioUrl(3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun audioUrlRejectsInvalidSurahNumber() {
        read.audioUrl(0)
    }
}
