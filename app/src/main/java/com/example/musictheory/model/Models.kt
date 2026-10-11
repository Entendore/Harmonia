package com.example.musictheory.model
import kotlinx.serialization.Serializable
@Serializable enum class Category { WESTERN, EASTERN, WORLD }
@Serializable data class TheoryTopic(val title: String, val tagline: String, val body: String,
    val id: String = "", val category: Category = Category.WESTERN, val related: List<String> = emptyList())
@Serializable data class Question(val text: String, val options: List<String>, val answer: Int,
    val explanation: String, val id: String = "", val category: String = "western",
    val tags: List<String> = emptyList(), val topicId: String? = null)
@Serializable data class MatchQuestion(val concept: String, val correct: String,
    val distractors: List<String>, val explanation: String, val id: String = "")
@Serializable data class PhraseItem(val name: String, val family: String,
    val semitones: List<Double>, val explanation: String)
@Serializable data class ScaleDemo(val name: String, val semitones: List<Double>, val note: String = "",
    val withDrone: Boolean = false, val asChord: Boolean = false,
    val timbre: com.example.musictheory.audio.TonePlayer.Timbre
        = com.example.musictheory.audio.TonePlayer.Timbre.SINE)
@Serializable data class RhythmPattern(val name: String, val slots: Int, val slotMs: Long,
    val accents: List<Int>, val note: String)
@Serializable data class ListenItem(val title: String, val query: String)
@Serializable data class GlossaryTerm(val term: String, val definition: String)
@Serializable data class ContrastPair(val target: String, val targetSemis: List<Double>,
    val other: String, val otherSemis: List<Double>)
@Serializable data class PathStep(val kind: Kind, val key: String, val label: String, val gameName: String = "") {
    @Serializable enum class Kind { TOPIC, GAME }
}
@Serializable data class LearningPath(val id: String, val emoji: String, val title: String,
    val blurb: String, val steps: List<PathStep>)
