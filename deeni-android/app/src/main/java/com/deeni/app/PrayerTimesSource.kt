package com.deeni.app

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class PrayerTimesResult(
    val dateLabel: String,
    val times: List<Pair<String, String>>,
    val latitude: Double,
    val longitude: Double
)

object PrayerTimesSource {
    fun load(city: String): Result<PrayerTimesResult> = runCatching {
        val encodedCity = URLEncoder.encode(city, "UTF-8")
        val encodedCountry = URLEncoder.encode("Saudi Arabia", "UTF-8")
        val endpoint = "https://api.aladhan.com/v1/timingsByCity?city=$encodedCity&country=$encodedCountry&method=4"
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 12000
        connection.readTimeout = 15000
        connection.setRequestProperty("Accept", "application/json")
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            check(code in 200..299) { "Prayer times service returned HTTP $code" }
            val envelope = JSONObject(body)
            check(envelope.optInt("code") == 200) { "Prayer times service returned an error" }
            val data = envelope.getJSONObject("data")
            val timings = data.getJSONObject("timings")
            val date = data.getJSONObject("date")
            val gregorian = date.getJSONObject("gregorian").optString("date")
            val hijri = date.getJSONObject("hijri").optString("date")
            val labels = listOf(
                "الفجر" to "Fajr",
                "الشروق" to "Sunrise",
                "الظهر" to "Dhuhr",
                "العصر" to "Asr",
                "المغرب" to "Maghrib",
                "العشاء" to "Isha"
            )
            val values = labels.map { (arabic, key) ->
                val raw = timings.optString(key, "")
                check(raw.isNotBlank() && raw != "null") { "Missing prayer time: $key" }
                arabic to raw.substringBefore(" ").trim()
            }
            val meta = data.getJSONObject("meta")
            val latitude = meta.getDouble("latitude")
            val longitude = meta.getDouble("longitude")
            PrayerTimesResult("الميلادي: $gregorian   •   الهجري: $hijri", values, latitude, longitude)
        } finally {
            connection.disconnect()
        }
    }
}
