package com.deeni.app

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Retrieves a single Quran page from the public AlQuran Cloud API.
 * The caller must run this on a worker thread.
 */
object QuranPageSource {
    fun loadAyahDetails(surah: Int, ayah: Int): Result<Pair<String, Int>> = runCatching {
        QuranReference.requireValid(surah, ayah)
        val connection = (URL("https://api.alquran.cloud/v1/ayah/$surah:$ayah/quran-uthmani").openConnection() as HttpURLConnection)
        connection.requestMethod = "GET"
        connection.connectTimeout = 12000
        connection.readTimeout = 15000
        connection.setRequestProperty("Accept", "application/json")
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            check(code in 200..299) { "Quran service returned HTTP $code" }
            val envelope = JSONObject(body)
            check(envelope.optString("code") == "200") { "Quran service response was not successful" }
            val data = envelope.getJSONObject("data")
            data.getString("text") to data.getInt("page")
        } finally {
            connection.disconnect()
        }
    }

    fun loadAyah(surah: Int, ayah: Int): Result<String> = runCatching {
        QuranReference.requireValid(surah, ayah)
        val connection = (URL("https://api.alquran.cloud/v1/ayah/$surah:$ayah/quran-uthmani").openConnection() as HttpURLConnection)
        connection.requestMethod = "GET"
        connection.connectTimeout = 12000
        connection.readTimeout = 15000
        connection.setRequestProperty("Accept", "application/json")
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            check(code in 200..299) { "Quran service returned HTTP $code" }
            val envelope = JSONObject(body)
            check(envelope.optString("code") == "200") { "Quran service response was not successful" }
            val data = envelope.getJSONObject("data")
            data.getString("text")
        } finally {
            connection.disconnect()
        }
    }

    fun load(page: Int): Result<String> = runCatching {
        require(page in 1..604) { "Page must be between 1 and 604" }
        val connection = (URL(QuranPageRequest.pageUrl(page)).openConnection() as HttpURLConnection)
        connection.requestMethod = "GET"
        connection.connectTimeout = 12000
        connection.readTimeout = 15000
        connection.setRequestProperty("Accept", "application/json")
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            check(code in 200..299) { "Quran service returned HTTP $code" }
            val envelope = JSONObject(body)
            check(envelope.optString("code") == "200") { "Quran service response was not successful" }
            val data = envelope.getJSONObject("data")
            val ayahs = data.getJSONArray("ayahs")
            val output = StringBuilder()
            var lastSurah = -1
            for (i in 0 until ayahs.length()) {
                val ayah = ayahs.getJSONObject(i)
                val surah = ayah.getJSONObject("surah")
                val surahNumber = surah.getInt("number")
                if (surahNumber != lastSurah) {
                    if (output.isNotEmpty()) output.append("\n\n")
                    output.append("۞  ").append(surah.getString("name")).append("  ۞\n")
                    lastSurah = surahNumber
                }
                output.append(ayah.getString("text"))
                output.append("  ﴿").append(ayah.getInt("numberInSurah")).append("﴾  ")
            }
            output.toString().trim()
        } finally {
            connection.disconnect()
        }
    }
}
