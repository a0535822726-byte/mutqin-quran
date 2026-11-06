package com.deeni.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranWordGeometryTest {
    private val pageOneSample = """
        {
          "page": 1,
          "coords": {
            "1:1:1": {"h": {"x": 667, "y": 175, "w": 213, "h": 68}},
            "1:1:2": {"h": {"x": 578, "y": 175, "w": 89, "h": 68}}
          }
        }
    """.trimIndent()

    @Test fun parsesWordIdsAndHighlightBounds() {
        val boxes = QuranWordGeometry.parsePage(1, pageOneSample)
        assertEquals(2, boxes.size)
        assertEquals(QuranWordBox(1, 1, 1, 667, 175, 213, 68), boxes["1:1:1"])
        assertEquals(2, boxes["1:1:2"]?.word)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsWrongPagePayload() {
        QuranWordGeometry.parsePage(2, pageOneSample)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutOfPageBounds() {
        val invalid = pageOneSample.replace("\"x\": 667", "\"x\": 899")
        QuranWordGeometry.parsePage(1, invalid)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidQuranWordReference() {
        val invalid = pageOneSample.replace("1:1:1", "1:99:1")
        QuranWordGeometry.parsePage(1, invalid)
    }

    @Test fun scalesCoordinatesWithoutChangingAspectRatio() {
        val box = QuranWordBox(1, 1, 1, 450, 718, 90, 68)
        val scaled = box.scaleTo(900, 1600)
        assertEquals(900f / 1437f, scaled.width / 90f, 0.0001f)
        assertTrue(scaled.x >= 0f)
        assertTrue(scaled.y >= 0f)
    }
}
