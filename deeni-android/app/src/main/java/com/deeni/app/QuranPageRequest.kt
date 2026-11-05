package com.deeni.app

internal object QuranPageRequest {
    fun pageUrl(page: Int): String {
        require(page in 1..604) { "Page must be between 1 and 604" }
        return "https://api.alquran.cloud/v1/page/$page/quran-uthmani"
    }
}
