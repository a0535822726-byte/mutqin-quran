package com.deeni.app

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class QuranSearchResult(
    val surahName: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val text: String
)

object QuranSearchSource {
    fun search(query: String): Result<List<QuranSearchResult>> = runCatching {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val connection = URL("https://api.alquran.cloud/v1/search/$encoded/all/ar").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 12000
        connection.readTimeout = 15000
        connection.setRequestProperty("Accept", "application/json")
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            check(code in 200..299) { "Quran search returned HTTP $code" }
            val envelope = JSONObject(body)
            check(envelope.optInt("code") == 200) { "Quran search was not successful" }
            val matches = envelope.getJSONObject("data").getJSONArray("matches")
            buildList {
                for (i in 0 until matches.length()) {
                    val item = matches.getJSONObject(i)
                    val surah = item.getJSONObject("surah")
                    add(
                        QuranSearchResult(
                            surahName = surah.getString("name"),
                            surahNumber = surah.getInt("number"),
                            ayahNumber = item.getInt("numberInSurah"),
                            text = item.getString("text")
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
