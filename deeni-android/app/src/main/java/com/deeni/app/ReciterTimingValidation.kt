package com.deeni.app

internal object ReciterTimingValidation {
    fun validate(surah: Int, timings: List<AyahTiming>): List<AyahTiming> {
        require(surah in 1..114) { "Surah number must be between 1 and 114" }
        require(timings.isNotEmpty()) { "No ayah timings were returned" }
        val sorted = timings.sortedBy { it.startMs }
        val seenAyahs = HashSet<Int>()
        var previousStart = -1
        for (timing in sorted) {
            QuranReference.requireValid(surah, timing.ayah)
            require(timing.startMs >= 0 && timing.endMs > timing.startMs) {
                "Invalid ayah timing range"
            }
            require(seenAyahs.add(timing.ayah)) { "Duplicate ayah timing" }
            require(timing.startMs >= previousStart) { "Ayah timings are not ordered" }
            previousStart = timing.startMs
        }
        return sorted
    }
}
