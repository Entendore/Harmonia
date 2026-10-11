package com.example.musictheory.data
import android.content.Context
import com.example.musictheory.data.db.*
import com.example.musictheory.model.Question
import kotlinx.coroutines.*; import kotlinx.coroutines.flow.*
data class HomeStats(val topicsStudied:Int=0, val gamesPlayed:Int=0, val recent:List<GameResult>=emptyList())
object ProgressRepo {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private fun dao(c: Context) = ProgressDb.get(c).dao()
    fun markRead(c: Context, key: String, title: String) = scope.launch { val d = dao(c); val p = d.topic(key)
        d.upsertTopic(TopicProgress(key, title, (p?.readCount ?: 0) + 1, p?.demoPlays ?: 0, System.currentTimeMillis())) }
    fun recordDemo(c: Context, key: String, title: String) = scope.launch { val d = dao(c); val p = d.topic(key)
        d.upsertTopic(TopicProgress(key, title, p?.readCount ?: 0, (p?.demoPlays ?: 0) + 1, System.currentTimeMillis())) }
    fun recordGame(c: Context, game: String, score: Int) = scope.launch {
        dao(c).insertResult(GameResult(game = game, score = score, playedAt = System.currentTimeMillis())) }
    fun recordAnswer(c: Context, game: String, item: String, correct: Boolean) = scope.launch {
        dao(c).insertAnswer(AnswerStat(game = game, item = item, correct = if (correct) 1 else 0, playedAt = System.currentTimeMillis())) }
    fun accuracy(c: Context, g: String): Flow<Float> = dao(c).recentAnswers(g).map { l ->
        if (l.isEmpty()) 0f else l.sumOf { it.correct }.toFloat() / l.size }
    suspend fun roundCount(c: Context, g: String) = dao(c).answerCount(g)
    fun homeStats(c: Context): Flow<HomeStats> = combine(dao(c).topicsStudied(), dao(c).gamesPlayed(), dao(c).allResults()) { s, g, r -> HomeStats(s, g, r.take(5)) }
    fun allTopics(c: Context) = dao(c).allTopics()
    fun studiedKeys(c: Context): Flow<Set<String>> = allTopics(c).map { l -> l.filter { it.readCount > 0 }.map { it.topicKey }.toSet() }
    fun gamesCount(c: Context): Flow<Int> = dao(c).gamesPlayed()
    fun playedGames(c: Context): Flow<Set<String>> = dao(c).playedGames().map { it.toSet() }
    fun allResults(c: Context) = dao(c).allResults()
    fun patterns(c: Context) = dao(c).allPatterns()
    suspend fun savePattern(c: Context, name: String, slots: Int, slotMs: Int, pattern: String) {
        dao(c).insertPattern(UserPattern(name = name, slots = slots, slotMs = slotMs, pattern = pattern, createdAt = System.currentTimeMillis())) }
    suspend fun deletePattern(c: Context, p: UserPattern) = dao(c).deletePattern(p)
    fun pathsCompleted(c: Context): Flow<Int> = dao(c).allPathProgress().map { l -> l.count { p ->
        (PathsData.byId[p.pathId]?.steps?.size ?: 0) > 0 && p.completedSteps >= (PathsData.byId[p.pathId]?.steps?.size ?: 0) } }
    suspend fun syncPath(c: Context, pathId: String, doneSteps: Int) { val d = dao(c)
        val total = PathsData.byId[pathId]?.steps?.size ?: return; val fin = doneSteps >= total
        val cur = d.pathProgress(pathId)
        if (cur == null || cur.completedSteps != doneSteps || (fin && cur.completedAt == 0L))
            d.upsertPathProgress(PathProgress(pathId, doneSteps, if (fin) System.currentTimeMillis() else 0L)) }
    suspend fun syncAllPaths(c: Context) { val (st, pl) = combine(studiedKeys(c), playedGames(c)) { a, b -> a to b }.first()
        val d = dao(c); val ex = d.allPathProgressOnce().associateBy { it.pathId }
        PathsData.all.forEach { p -> val done = PathEngine.completedCount(p, PathEngine.Snapshot(st, pl))
            val fin = done >= p.steps.size; val cur = ex[p.pathId]
            if (cur == null || cur.completedSteps != done || (fin && cur.completedAt == 0L))
                d.upsertPathProgress(PathProgress(p.pathId, done, if (fin) System.currentTimeMillis() else 0L)) } }
    fun allPathProgress(c: Context) = dao(c).allPathProgress()
    suspend fun buildReviewSet(c: Context, size: Int = 10): List<Pair<Question, String>> { val d = dao(c); val now = System.currentTimeMillis()
        val picked = d.due(now, size).filter { ContentRepo.questionById(it.ref) != null }.toMutableList()
        if (picked.size < size) { val ex = d.allRefs().toSet()
            ContentRepo.questions.map { it.id }.filter { it !in ex }.shuffled().take(size - picked.size)
                .forEach { id -> val card = LeitnerCard(id, 0, now, 0); d.upsertCard(card); picked.add(card) } }
        return picked.mapNotNull { cd -> ContentRepo.questionById(cd.ref)?.let { it to cd.ref } } }
    fun grade(c: Context, ref: String, correct: Boolean) = scope.launch { val d = dao(c)
        val cur = d.byRef(ref) ?: LeitnerCard(ref, 0, 0, 0); d.upsertCard(LeitnerMath.graded(cur, correct, System.currentTimeMillis())) }
    fun dueCount(c: Context): Flow<Int> = dao(c).allCards().map { l -> val now = System.currentTimeMillis(); l.count { it.dueAt <= now } }
    suspend fun migrateLeitnerRefs(c: Context, idByLegacyIndex: List<String>) = withContext(Dispatchers.IO) {
        val d = ProgressDb.get(c).dao()
        d.allCardsOnce().forEach { card -> val n = card.ref.toIntOrNull()
            when { n == null -> {}
                n < idByLegacyIndex.size -> d.upsertCard(card.copy(ref = idByLegacyIndex[n]))
                else -> d.deleteCard(card.ref) } } }
}
