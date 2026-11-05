package com.deeni.app

import org.junit.Assert.assertEquals
import org.junit.Test

class QiblaMathTest {
    @Test
    fun makkahCoordinatesPointToZeroDegrees() {
        assertEquals(0.0, QiblaMath.bearing(21.4225, 39.8262), 0.0001)
    }

    @Test
    fun abuArishBearingIsNorthwest() {
        assertEquals(327.98, QiblaMath.bearing(16.97, 42.83), 0.1)
    }

    @Test
    fun londonBearingIsSoutheast() {
        assertEquals(118.99, QiblaMath.bearing(51.5074, -0.1278), 0.1)
    }

    @Test
    fun relativeAngleWrapsAcrossNorthInBothDirections() {
        assertEquals(-20f, QiblaMath.relativeAngle(350f, 10f), 0.001f)
        assertEquals(20f, QiblaMath.relativeAngle(10f, 350f), 0.001f)
    }

    @Test
    fun relativeAngleAcceptsNegativeSensorAzimuth() {
        assertEquals(10f, QiblaMath.relativeAngle(0f, -10f), 0.001f)
        assertEquals(-10f, QiblaMath.relativeAngle(-10f, 0f), 0.001f)
    }

    @Test
    fun relativeAngleStaysWithinHalfTurn() {
        val angle = QiblaMath.relativeAngle(270f, 90f)
        assertEquals(180f, kotlin.math.abs(angle), 0.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsLatitudeOutsideEarthRange() {
        QiblaMath.bearing(91.0, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsLongitudeOutsideEarthRange() {
        QiblaMath.bearing(0.0, 181.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonFiniteCoordinates() {
        QiblaMath.bearing(Double.NaN, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonFiniteCompassAngles() {
        QiblaMath.relativeAngle(Float.NaN, 0f)
    }
}
