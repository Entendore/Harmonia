package com.example.musictheory.data
data class AchSnap(val studied: Set<String> = emptySet(), val played: Set<String> = emptySet(),
    val gamesCount: Int = 0, val bests: Map<String, Int> = emptyMap(), val streak: Int = 0, val pathsDone: Int = 0)
data class Achievement(val emoji: String, val title: String, val desc: String, val test: (AchSnap) -> Boolean)
object Achievements {
    val all = listOf(
        Achievement("🌱","First Steps","Study your first topic"){ it.studied.isNotEmpty() },
        Achievement("🏛","Western Scholar","Study 5 Western topics"){ it.studied.count{ k->k.startsWith("WESTERN")}>=5 },
        Achievement("🪷","Eastern Scholar","Study 8 Eastern topics"){ it.studied.count{ k->k.startsWith("EASTERN")}>=8 },
        Achievement("🌍","World Citizen","Study 8 World topics"){ it.studied.count{ k->k.startsWith("WORLD")}>=8 },
        Achievement("🗣","Polyglot","Open all three collections"){ listOf("WESTERN","EASTERN","WORLD")
            .all{ p-> it.studied.any{ k->k.startsWith(p)} } },
        Achievement("👂","Golden Ear","8+ streak in Note Hunt"){ (it.bests["best_note"] ?: 0) >= 8 },
        Achievement("🎼","Quarter-tone Native","60+ points in Raga & Maqam"){ (it.bests["best_ragamaqam"] ?: 0) >= 60 },
        Achievement("🥁","Metronome Mind","300+ points in Rhythm Tap"){ (it.bests["best_rhythm"] ?: 0) >= 300 },
        Achievement("🔥","On Fire","3-day Daily Challenge streak"){ it.streak >= 3 },
        Achievement("🏅","Marathoner","Play 20 games"){ it.gamesCount >= 20 },
        Achievement("🧠","Perfect Ten","Score 100 in the Theory Quiz"){ (it.bests["best_quiz"] ?: 0) >= 100 },
        Achievement("🗺","Trailblazer","Complete a learning path"){ it.pathsDone >= 1 })
}
