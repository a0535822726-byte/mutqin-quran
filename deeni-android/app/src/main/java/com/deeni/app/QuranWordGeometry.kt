package com.deeni.app

import org.json.JSONObject

/** Pixel bounds for one Quran word in the 900x1437 source coordinate space. */
internal data class QuranWordBox(
    val surah: Int,
    val ayah: Int,
    val word: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    fun scaleTo(targetWidth: Int, targetHeight: Int): QuranScaledWordBox {
        require(targetWidth > 0 && targetHeight > 0) { "Target dimensions must be positive" }
        val scale = minOf(
            targetWidth.toFloat() / QuranWordGeometry.SOURCE_WIDTH,
            targetHeight.toFloat() / QuranWordGeometry.SOURCE_HEIGHT
        )
        val renderedWidth = QuranWordGeometry.SOURCE_WIDTH * scale
        val renderedHeight = QuranWordGeometry.SOURCE_HEIGHT * scale
        val offsetX = (targetWidth - renderedWidth) / 2f
        val offsetY = (targetHeight - renderedHeight) / 2f
        return QuranScaledWordBox(
            x = offsetX + x * scale,
            y = offsetY + y * scale,
            width = width * scale,
            height = height * scale
        )
    }
}

internal data class QuranScaledWordBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

/**
 * Strict parser for page coordinate payloads keyed as surah:ayah:word.
 * Source geometry is only usable when the rendered mushaf edition/page is verified
 * to match the coordinate dataset; parsing alone does not establish that match.
 */
internal object QuranWordGeometry {
    const val SOURCE_WIDTH = 900
    const val SOURCE_HEIGHT = 1437

    fun parsePage(requestedPage: Int, json: String): Map<String, QuranWordBox> {
        require(requestedPage in 1..604) { "Page must be between 1 and 604" }
        val root = JSONObject(json)
        require(root.getInt("page") == requestedPage) { "Coordinate data belongs to a different page" }
        val coords = root.getJSONObject("coords")
        val result = LinkedHashMap<String, QuranWordBox>()
        val keys = coords.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val parts = key.split(':')
            require(parts.size == 3) { "Invalid Quran word key: $key" }
            val surah = parts[0].toIntOrNull() ?: error("Invalid surah in key: $key")
            val ayah = parts[1].toIntOrNull() ?: error("Invalid ayah in key: $key")
            val word = parts[2].toIntOrNull() ?: error("Invalid word index in key: $key")
            QuranReference.requireValid(surah, ayah)
            require(word > 0) { "Word index must be positive" }

            val box = coords.getJSONObject(key).getJSONObject("h")
            val x = box.getInt("x")
            val y = box.getInt("y")
            val width = box.getInt("w")
            val height = box.getInt("h")
            require(x >= 0 && y >= 0 && width > 0 && height > 0) { "Invalid word bounds for $key" }
            require(x + width <= SOURCE_WIDTH && y + height <= SOURCE_HEIGHT) {
                "Word bounds exceed the source page for $key"
            }
            require(result.put(key, QuranWordBox(surah, ayah, word, x, y, width, height)) == null) {
                "Duplicate Quran word key: $key"
            }
        }
        require(result.isNotEmpty()) { "Coordinate page contains no word boxes" }
        return result
    }
}
