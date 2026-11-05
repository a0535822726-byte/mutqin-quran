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
        QuranReference.requireValid(surah, ayah)
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
            val editions = envelope.getJSONArray("data")
            check(editions.length() >= 2) { "Tafsir editions are unavailable" }
            val quran = editions.getJSONObject(0)
            val tafsir = editions.getJSONObject(1)
            val returnedSurah = quran.getJSONObject("surah").getInt("number")
            val returnedAyah = quran.getInt("numberInSurah")
            TafsirValidation.result(
                requestedSurah = surah,
                requestedAyah = ayah,
                returnedSurah = returnedSurah,
                returnedAyah = returnedAyah,
                surahName = quran.getJSONObject("surah").getString("name"),
                ayahText = quran.getString("text"),
                tafsirText = tafsir.getString("text")
            )
        } finally {
            connection.disconnect()
        }
    }
}
