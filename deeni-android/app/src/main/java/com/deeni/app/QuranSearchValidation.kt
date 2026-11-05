package com.deeni.app

internal object QuranSearchValidation {
    fun query(value: String): String {
        val normalized = value.trim()
        require(normalized.isNotEmpty()) { "Search query is required" }
        require(normalized.length <= 120) { "Search query is too long" }
        return normalized
    }

    fun result(surahName: String, surah: Int, ayah: Int, text: String): QuranSearchResult {
        QuranReference.requireValid(surah, ayah)
        require(surahName.isNotBlank()) { "Missing surah name in search result" }
        require(text.isNotBlank()) { "Missing ayah text in search result" }
        return QuranSearchResult(surahName, surah, ayah, text)
    }
}
