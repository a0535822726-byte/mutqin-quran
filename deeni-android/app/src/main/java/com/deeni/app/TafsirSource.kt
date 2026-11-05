package com.deeni.app

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class TafsirResult(
    val surahName: String,
    val ayahNumber: Int,
    val ayahText: String,
    val tafsirText: String
)

object TafsirSource {
    fun load(surah: Int, ayah: Int): Result<TafsirResult> = runCatching {
        require(surah in 1..114) { "Surah number must be between 1 and 114" }
        require(ayah in 1..286) { "Ayah number is out of range" }
        val endpoint = "https://api.alquran.cloud/v1/ayah/$surah:$ayah/editions/quran-uthmani,ar.muyassar"
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 12000
        connection.readTimeout = 15000
        connection.setRequestProperty("Accept", "application/json")
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            check(code in 200..299) { "Tafsir service returned HTTP $code" }
            val envelope = JSONObject(body)
            check(envelope.optString("code") == "200") { "Tafsir service returned an error" }
            val editions = envelope.getJSONObject("data").getJSONArray("editions")
            check(editions.length() >= 2) { "Tafsir editions are unavailable" }
            val quran = editions.getJSONObject(0)
            val tafsir = editions.getJSONObject(1)
            TafsirResult(
                quran.getJSONObject("surah").getString("name"),
                quran.getInt("numberInSurah"),
                quran.getString("text"),
                tafsir.getString("text")
            )
        } finally {
            connection.disconnect()
        }
    }
}
