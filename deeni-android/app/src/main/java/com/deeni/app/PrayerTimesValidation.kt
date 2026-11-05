package com.deeni.app

/** Validates untrusted values returned by the prayer-times service before display/use. */
internal object PrayerTimesValidation {
    fun time(raw: String, key: String): String {
        val value = raw.substringBefore(" ").trim()
        require(Regex("(?:[01]\\d|2[0-3]):[0-5]\\d").matches(value)) {
            "Invalid prayer time: $key"
        }
        return value
    }

    fun coordinates(latitude: Double, longitude: Double): Pair<Double, Double> {
        require(latitude.isFinite() && latitude in -90.0..90.0) {
            "Invalid prayer-times latitude"
        }
        require(longitude.isFinite() && longitude in -180.0..180.0) {
            "Invalid prayer-times longitude"
        }
        return latitude to longitude
    }
}
