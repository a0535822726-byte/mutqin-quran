package com.deeni.app

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object QuranSearchSource {
    fun search(query: String): Result<List<Triple<String, Int, String>>> = runCatching {
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
            check(envelope.optString("code") == "200") { "Quran search was not successful" }
            val matches = envelope.getJSONObject("data").getJSONArray("matches")
            buildList {
                for (i in 0 until matches.length()) {
                    val item = matches.getJSONObject(i)
                    val surah = item.getJSONObject("surah").getString("name")
                    val number = item.getInt("numberInSurah")
                    val text = item.getString("text")
                    add(Triple(surah, number, text))
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
