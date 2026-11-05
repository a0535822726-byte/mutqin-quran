package com.deeni.app

/** Validates verse identity and non-empty text returned by the tafsir service. */
internal object TafsirValidation {
    fun result(
        requestedSurah: Int,
        requestedAyah: Int,
        returnedSurah: Int,
        returnedAyah: Int,
        tafsirSurah: Int,
        tafsirAyah: Int,
        surahName: String,
        ayahText: String,
        tafsirText: String
    ): TafsirResult {
        QuranReference.requireValid(requestedSurah, requestedAyah)
        require(returnedSurah == requestedSurah && returnedAyah == requestedAyah) {
            "Quran edition returned a different ayah"
        }
        require(tafsirSurah == requestedSurah && tafsirAyah == requestedAyah) {
            "Tafsir edition returned a different ayah"
        }
        require(surahName.isNotBlank()) { "Missing surah name" }
        require(ayahText.isNotBlank()) { "Missing Quran ayah text" }
        require(tafsirText.isNotBlank()) { "Missing tafsir text" }
        return TafsirResult(surahName, returnedAyah, ayahText, tafsirText)
    }
}
