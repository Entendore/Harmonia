package com.example.musictheory.util
object RhythmGrid {
    /** Absolute-time step scheduler — immune to cumulative execution jitter. */
    suspend fun run(slots: Int, loops: Int, slotMs: Long, startAt: Long, now: () -> Long,
                    wait: suspend (Long) -> Unit, onStep: (Int) -> Unit) {
        val total = slots * loops
        for (i in 0 until total) { val t = startAt + i * slotMs
            wait((t - now()).coerceAtLeast(1L)); onStep(i % slots) } }
}
