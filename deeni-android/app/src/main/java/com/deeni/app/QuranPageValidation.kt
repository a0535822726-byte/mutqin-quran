package com.deeni.app

/** Checks Quran API payload metadata before its contents are rendered. */
internal object QuranPageValidation {
    fun pageNumber(requested: Int, returned: Int): Int {
        require(requested in 1..604) { "Requested page is outside the mushaf" }
        require(returned == requested) { "Quran service returned a different page" }
        return returned
    }

    fun ayahText(surah: Int, ayah: Int, text: String): String {
        QuranReference.requireValid(surah, ayah)
        require(text.isNotBlank()) { "Quran service returned empty ayah text" }
        return text
    }
}
