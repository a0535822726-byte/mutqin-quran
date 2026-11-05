package com.deeni.app

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Small, platform-independent calculations shared by the Qibla screen and unit tests.
 */
internal object QiblaMath {
    private const val KAABA_LATITUDE = 21.4225
    private const val KAABA_LONGITUDE = 39.8262

    /** Returns the initial great-circle bearing from the supplied coordinates, clockwise from true north. */
    fun bearing(latitude: Double, longitude: Double): Double {
        require(latitude.isFinite() && latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude.isFinite() && longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }

        val kaabaLat = Math.toRadians(KAABA_LATITUDE)
        val kaabaLon = Math.toRadians(KAABA_LONGITUDE)
        val userLat = Math.toRadians(latitude)
        val deltaLon = kaabaLon - Math.toRadians(longitude)
        val y = sin(deltaLon)
        val x = cos(userLat) * tan(kaabaLat) - sin(userLat) * cos(deltaLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /** Smallest signed angle from device heading to Qibla, normalized to [-180, 180]. */
    fun relativeAngle(qiblaBearing: Float, deviceAzimuth: Float): Float {
        require(qiblaBearing.isFinite() && deviceAzimuth.isFinite()) { "Angles must be finite" }
        return ((qiblaBearing - deviceAzimuth + 540f) % 360f) - 180f
    }
}
