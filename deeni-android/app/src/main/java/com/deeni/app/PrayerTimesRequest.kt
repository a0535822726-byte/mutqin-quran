package com.deeni.app

import java.net.URLEncoder

internal object PrayerTimesRequest {
    private val supportedMethods = setOf(1, 2, 3, 4, 5, 8, 9, 10, 11, 13, 15)

    fun url(city: String, country: String, method: Int): String {
        require(city.isNotBlank()) { "City is required" }
        require(country.isNotBlank()) { "Country is required" }
        require(method in supportedMethods) { "Unsupported calculation method" }
        val encodedCity = URLEncoder.encode(city.trim(), "UTF-8")
        val encodedCountry = URLEncoder.encode(country.trim(), "UTF-8")
        return "https://api.aladhan.com/v1/timingsByCity?city=$encodedCity&country=$encodedCountry&method=$method"
    }
}
