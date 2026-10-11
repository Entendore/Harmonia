package com.example.musictheory.data
import com.example.musictheory.model.*
import kotlinx.serialization.Serializable; import kotlinx.serialization.json.Json
import java.io.InputStream
@Serializable data class ContentManifest(val schemaVersion: Int, val contentVersion: Int, val counts: Map<String, Int>)
object ContentRepo {
    const val SCHEMA_VERSION = 1
    private const val BASE = "content"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    @Volatile private var loaded = false; val isLoaded: Boolean get() = loaded
    lateinit var manifest: ContentManifest; private set
    lateinit var topics: List<TheoryTopic>; private set
    lateinit var questions: List<Question>; private set
    lateinit var matchQuestions: List<MatchQuestion>; private set
    lateinit var phrases: List<PhraseItem>; private set
    lateinit var glossary: List<GlossaryTerm>; private set
    lateinit var facts: List<String>; private set
    lateinit var listenLinks: Map<String, List<ListenItem>>; private set
    lateinit var listenCues: Map<String, String>; private set
    lateinit var scaleDemos: Map<String, List<ScaleDemo>>; private set
    lateinit var rhythmPatterns: List<RhythmPattern>; private set
    lateinit var paths: List<LearningPath>; private set
    lateinit var contrasts: List<ContrastPair>; private set
    fun load(openFile: (String) -> InputStream) { if (loaded) return; synchronized(this) {
        if (loaded) return
        manifest = json.decodeFromString(txt("manifest.json"))
        check(manifest.schemaVersion == SCHEMA_VERSION) { "content schema mismatch" }
        topics = list("topics/western.json") + list("topics/eastern.json") + list("topics/world.json")
        questions = list("questions/all.json"); matchQuestions = list("match/match.json")
        phrases = list("phrases/phrases.json"); glossary = list("glossary.json"); facts = list("facts.json")
        listenLinks = mapL("listen/links.json"); listenCues = mapS("listen/cues.json")
        scaleDemos = mapL("demos/scales.json"); rhythmPatterns = list("rhythm/patterns.json")
        paths = list("paths/paths.json"); contrasts = list("compare.json")
        validate(); loaded = true } }
    fun topicsFor(cat: Category) = topics.filter { it.category == cat }
    fun topicById(id: String) = topics.firstOrNull { it.id == id }
    fun questionById(id: String) = questions.firstOrNull { it.id == id }
    fun questionsForTopic(t: String) = questions.filter { it.topicId == t }
    fun questionsByCategory(c: String) = questions.filter { it.category == c }
    fun questionsByTag(t: String) = questions.filter { t in it.tags }
    private fun txt(rel: String) = openFile("$BASE/$rel").bufferedReader().use { it.readText() }
    private inline fun <reified T> list(rel: String): List<T> { val v = json.decodeFromString<List<T>>(txt(rel)); cnt(rel, v.size); return v }
    private inline fun <reified V> mapL(rel: String): Map<String, List<V>> { val v = json.decodeFromString<Map<String, List<V>>>(txt(rel)); cnt(rel, v.size); return v }
    private fun mapS(rel: String): Map<String, String> { val v = json.decodeFromString<Map<String, String>>(txt(rel)); cnt(rel, v.size); return v }
    private fun cnt(rel: String, n: Int) { val e = manifest.counts[rel] ?: return
        check(e == n) { "content/$rel has $n items; manifest says $e" } }
    private fun validate() {
        fun <T,K> List<T>.dupes(k: (T)->K) = groupBy(k).filterValues { it.size > 1 }.keys
        check(topics.dupes { it.id }.isEmpty()); check(questions.dupes { it.id }.isEmpty())
        check(matchQuestions.dupes { it.id }.isEmpty())
        questions.forEach { q -> check(q.answer in q.options.indices) { "${q.id}: answer range" }
            check(q.options.size == q.options.distinct().size) { "${q.id}: dup options" }
            check(q.category in setOf("western","eastern","world")) { "${q.id}: category" }
            q.topicId?.let { check(topicById(it) != null) { "${q.id}: topicId $it" } } }
        topics.forEach { t -> t.related.forEach { r -> check(topicById(r) != null) { "${t.id}: related $r" } } }
        phrases.forEach { p -> check(p.semitones.first() == 0.0 && p.semitones.all { it in 0.0..12.0 }) }
        contrasts.forEach { c -> check(c.targetSemis.size == c.otherSemis.size && c.targetSemis.first() == 0.0 && c.otherSemis.first() == 0.0) }
        rhythmPatterns.forEach { p -> check(p.slots > 0 && p.accents.all { it in 0 until p.slots } && p.accents.size == p.accents.distinct().size) }
        paths.forEach { p -> p.steps.forEach { s -> if (s.kind == PathStep.Kind.TOPIC) check(topicById(s.key) != null) { "${p.id}: step ${s.key}" } } }
        listenLinks.keys.forEach { check(topicById(it) != null) { "links: $it" } }
        scaleDemos.keys.forEach { check(topicById(it) != null) { "scales: $it" } } }
}
