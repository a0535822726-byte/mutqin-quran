package com.deeni.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ReciterRead(
    val reciterName: String,
    val readName: String,
    val readId: Int,
    val audioServer: String,
    val surahIds: Set<Int>,
    val timingReadId: Int?
) {
    fun audioUrl(surah: Int): String {
        require(surah in surahIds) { "This recitation does not include the selected surah" }
        return audioServer.trimEnd('/') + "/" + surah.toString().padStart(3, '0') + ".mp3"
    }
}

data class AyahTiming(val ayah: Int, val startMs: Int, val endMs: Int)

object ReciterSource {
    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 20000
        connection.setRequestProperty("Accept", "application/json")
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            check(code in 200..299) { "Audio catalog returned HTTP $code" }
            return body
        } finally {
            connection.disconnect()
        }
    }

    fun loadReciters(): Result<List<ReciterRead>> = runCatching {
        val timingRows = JSONArray(get("https://mp3quran.net/api/v3/ayat_timing/reads"))
        val timingByFolder = HashMap<String, Int>()
        for (i in 0 until timingRows.length()) {
            val row = timingRows.getJSONObject(i)
            val folder = row.optString("folder_url").trimEnd('/')
            if (folder.isNotBlank()) timingByFolder[folder] = row.optInt("id", -1)
        }

        val root = JSONObject(get("https://mp3quran.net/api/v3/reciters?language=ar"))
        val reciters = root.getJSONArray("reciters")
        val output = ArrayList<ReciterRead>()
        for (i in 0 until reciters.length()) {
            val reciter = reciters.getJSONObject(i)
            val name = reciter.optString("name").trim()
            val moshaf = reciter.optJSONArray("moshaf") ?: continue
            for (j in 0 until moshaf.length()) {
                val read = moshaf.getJSONObject(j)
                val readName = read.optString("name").trim()
                val isHafs = readName.contains("حفص") || readName.contains("Hafs", ignoreCase = true)
                if (!isHafs) continue
                val server = read.optString("server").trim()
                if (server.isBlank()) continue
                val ids = read.optString("surah_list").split(',')
                    .mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..114 }.toSet()
                if (ids.isEmpty()) continue
                output.add(
                    ReciterRead(
                        name,
                        readName,
                        read.optInt("id", reciter.optInt("id")),
                        server,
                        ids,
                        timingByFolder[server.trimEnd('/')]
                    )
                )
            }
        }
        output.distinctBy { it.audioServer.trimEnd('/') }.sortedBy { it.reciterName }
    }

    fun loadTimings(surah: Int, timingReadId: Int): Result<List<AyahTiming>> = runCatching {
        require(surah in 1..114)
        require(timingReadId > 0)
        val rows = JSONArray(get("https://mp3quran.net/api/v3/ayat_timing?surah=$surah&read=$timingReadId"))
        val timings = buildList {
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                add(
                    AyahTiming(
                        ayah = row.getInt("ayah"),
                        startMs = row.getInt("start_time"),
                        endMs = row.getInt("end_time")
                    )
                )
            }
        }
        ReciterTimingValidation.validate(surah, timings)
    }
}
