package com.example.musictheory.data
import android.content.Context; import android.content.Intent; import android.net.Uri
import com.example.musictheory.model.*
import com.example.musictheory.util.safeStart; import java.net.URLEncoder
object TheoryData { fun topicsFor(cat: Category) = ContentRepo.topicsFor(cat) }
object QuestionRepo { val all: List<Question> get() = ContentRepo.questions }
object QuestionSets {
    val options get() = listOf(
        Triple("all", "🎯 Everything", ContentRepo.questions.size),
        Triple("western", "🏛 Western only", ContentRepo.questionsByCategory("western").size),
        Triple("eastern", "🪷 Eastern only", ContentRepo.questionsByCategory("eastern").size),
        Triple("world", "🌍 World only", ContentRepo.questionsByCategory("world").size),
        Triple("rhythm", "🥁 Rhythm only", ContentRepo.questionsByTag("rhythm").size))
    fun build(id: String): List<Question> = when (id) {
        "western" -> ContentRepo.questionsByCategory("western")
        "eastern" -> ContentRepo.questionsByCategory("eastern")
        "world" -> ContentRepo.questionsByCategory("world")
        "rhythm" -> ContentRepo.questionsByTag("rhythm"); else -> ContentRepo.questions }
}
object MoreContent {
    fun allMatchQuestions(): List<Question> = ContentRepo.matchQuestions.map { m ->
        val opts = (m.distractors + m.correct).shuffled()
        Question("Which tradition does \u201C${m.concept}\u201D belong to?", opts, opts.indexOf(m.correct), m.explanation)
    }.shuffled().take(10)
}
object ExtraQuestions { val phrases get() = ContentRepo.phrases }
object Glossary { val terms get() = ContentRepo.glossary }
object Facts { val all get() = ContentRepo.facts }
object ScaleDemos { fun forTopic(cat: Category, index: Int): List<ScaleDemo> = ContentRepo.scaleDemos["${cat.name}_$index"].orEmpty() }
object ListenLinks {
    fun forTopic(key: String) = ContentRepo.listenLinks[key].orEmpty()
    fun open(context: Context, item: ListenItem) { context.safeStart(
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + URLEncoder.encode(item.query, "UTF-8"))),
        "No browser or YouTube app found") }
}
object ListenCues { fun forDemo(topicKey: String, demoName: String): String? = ContentRepo.listenCues["$topicKey|$demoName"] }
object RhythmPatterns { val all get() = ContentRepo.rhythmPatterns }
object PathsData {
    val all get() = ContentRepo.paths
    val byId get() = ContentRepo.paths.associateBy { it.id }
    val suggestion = mapOf("western" to "western_foundations", "raga" to "raga_journey", "rhythm" to "rhythm_passport")
}
object PathEngine {
    data class Snapshot(val studied: Set<String>, val played: Set<String>)
    fun stepDone(step: PathStep, s: Snapshot) = when (step.kind) {
        PathStep.Kind.TOPIC -> step.key in s.studied; PathStep.Kind.GAME -> step.gameName in s.played }
    fun completedCount(path: LearningPath, s: Snapshot): Int { var i = 0
        while (i < path.steps.size && stepDone(path.steps[i], s)) i++; return i }
}
object CompareData {
    val contrasts get() = ContentRepo.contrasts
    val contour = listOf(0, 1, 2, 1, 3, 2, 1, 0); val roots = listOf(0, 2, 5, 7, 9)
}
