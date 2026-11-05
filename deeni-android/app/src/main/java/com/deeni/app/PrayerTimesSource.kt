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
    fun load(city: String, country: String = "Saudi Arabia", method: Int = 4): Result<PrayerTimesResult> = runCatching {
        val endpoint = PrayerTimesRequest.url(city, country, method)
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
                arabic to PrayerTimesValidation.time(raw, key)
            }
            val meta = data.getJSONObject("meta")
            val latitude = meta.getDouble("latitude")
            val longitude = meta.getDouble("longitude")
            val (validLatitude, validLongitude) = PrayerTimesValidation.coordinates(latitude, longitude)
            PrayerTimesResult("الميلادي: $gregorian   •   الهجري: $hijri", values, validLatitude, validLongitude)
        } finally {
            connection.disconnect()
        }
    }
}
