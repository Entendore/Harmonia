package com.example.musictheory.vm
import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.musictheory.audio.Timbre
import com.example.musictheory.audio.TonePlayer
import com.example.musictheory.data.MoreContent
import com.example.musictheory.data.ProgressRepo
import com.example.musictheory.data.QuestionRepo
import com.example.musictheory.data.SettingsRepo
import com.example.musictheory.model.PhraseItem
import com.example.musictheory.model.Question
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.random.Random
class McqViewModel(app: Application, val bestKey: String, val gameName: String,
    private val load: () -> List<Question>, private val onFinishExtra: suspend () -> Unit = {}
) : AndroidViewModel(app) {
    data class Ui(val questions: List<Question> = emptyList(), val index: Int = 0,
        val score: Int = 0, val chosen: Int? = null, val finished: Boolean = false)
    private val _ui = MutableStateFlow(Ui(load()))
    val ui: StateFlow<Ui> = _ui.asStateFlow()
    init { viewModelScope.launch {
        ui.filter { it.finished }.collect {
            val ctx = getApplication<Application>()
            SettingsRepo.saveBest(ctx, bestKey, it.score)
            ProgressRepo.recordGame(ctx, gameName, it.score)
            onFinishExtra() } } }
    fun choose(i: Int) = _ui.update { s ->
        if (s.chosen != null || s.finished) return@update s
        val ok = i == s.questions[s.index].answer
        if (!ok) { val id = s.questions[s.index].id
            if (id.isNotEmpty()) ProgressRepo.grade(getApplication(), id, false) }
        s.copy(chosen = i, score = s.score + if (ok) 10 else 0) }
    fun next() = _ui.update { s -> when {
        s.finished || s.chosen == null -> s
        s.index < s.questions.lastIndex -> s.copy(index = s.index + 1, chosen = null)
        else -> s.copy(finished = true) } }
    fun replay() { _ui.value = Ui(load()) }
}
@Composable
fun rememberMcqViewModel(vmKey: String, bestKey: String, gameName: String,
    onFinishExtra: suspend () -> Unit = {}, load: () -> List<Question>): McqViewModel {
    val app = LocalContext.current.applicationContext as Application
    return viewModel(key = vmKey, factory = viewModelFactory {
        initializer { McqViewModel(app, bestKey, gameName, load, onFinishExtra) } })
}
abstract class EarGameViewModel<R>(app: Application, private val bestKey: String,
    private val gameName: String, val totalRounds: Int = 10) : AndroidViewModel(app) {
    data class Ui<R>(val round: Int = 1, val score: Int = 0, val finished: Boolean = false,
        val chosen: Int? = null, val options: List<String> = emptyList(),
        val answerIndex: Int = -1, val data: R? = null)
    private val _ui = MutableStateFlow(Ui(makeRound(1, 0)))
    val ui: StateFlow<Ui<R>> = _ui.asStateFlow()
    private var advanceJob: Job? = null
    protected var accuracy = 0f; protected var roundsPlayed = 0
    init { viewModelScope.launch {
        accuracy = ProgressRepo.accuracy(getApplication(), gameName).first()
        roundsPlayed = ProgressRepo.roundCount(getApplication(), gameName) } }
    private fun makeRound(round: Int, score: Int): Ui<R> { val d = newRound()
        return Ui(round, score, false, null, optionLabels(d), answerIndex(d), d) }
    protected abstract fun newRound(): R
    protected abstract fun optionLabels(d: R): List<String>
    protected abstract fun answerIndex(d: R): Int
    protected abstract fun itemTag(d: R): String
    fun choose(i: Int) { val s = _ui.value
        if (s.chosen != null || s.finished || advanceJob?.isActive == true) return
        _ui.update { it.copy(chosen = i, score = it.score + if (i == it.answerIndex) 10 else 0) }
        s.data?.let { ProgressRepo.recordAnswer(getApplication(), gameName, itemTag(it), i == s.answerIndex) }
        advanceJob = viewModelScope.launch { delay(1500); val cur = _ui.value
            if (cur.round < totalRounds) _ui.value = makeRound(cur.round + 1, cur.score)
            else { _ui.update { it.copy(finished = true) }
                val ctx = getApplication<Application>()
                SettingsRepo.saveBest(ctx, bestKey, _ui.value.score)
                ProgressRepo.recordGame(ctx, gameName, _ui.value.score) } } }
    fun restart() { advanceJob?.cancel()
        viewModelScope.launch {
            accuracy = ProgressRepo.accuracy(getApplication(), gameName).first()
            roundsPlayed = ProgressRepo.roundCount(getApplication(), gameName)
            _ui.value = makeRound(1, 0) } }
}
val earIntervals = listOf("Minor 2nd" to 1, "Major 2nd" to 2, "Minor 3rd" to 3, "Major 3rd" to 4,
    "Perfect 4th" to 5, "Tritone" to 6, "Perfect 5th" to 7, "Octave" to 12,
    "Minor 6th" to 8, "Major 6th" to 9, "Minor 7th" to 10)
class IntervalGameViewModel(app: Application) :
    EarGameViewModel<IntervalGameViewModel.D>(app, "best_interval", "Interval Ear Training") {
    data class D(val root: Int, val pool: List<Pair<String, Int>>, val idx: Int, val order: List<Int>)
    override fun newRound(): D {
        val pool = if (accuracy >= 0.75f && roundsPlayed >= 5) earIntervals else earIntervals.take(8)
        val idx = Random.nextInt(pool.size)
        return D(Random.nextInt(-5, 8), pool, idx, (pool.indices.filter { it != idx } + idx).shuffled()) }
    override fun optionLabels(d: D) = d.order.map { d.pool[it].first }
    override fun answerIndex(d: D) = d.order.indexOf(d.idx)
    override fun itemTag(d: D) = d.pool[d.idx].first
    fun play() { val d = ui.value.data ?: return
        TonePlayer.playFrequencies(listOf(TonePlayer.Notes.freqD(d.root.toDouble()),
            TonePlayer.Notes.freqD((d.root + d.pool[d.idx].second).toDouble()))) }
}
val noteKeys = listOf("C4" to 261.63, "D4" to 293.66, "E4" to 329.63, "F4" to 349.23,
    "G4" to 392.00, "A4" to 440.00, "B4" to 493.88)
class NoteGameViewModel(app: Application) : EarGameViewModel<Int>(app, "best_note", "Note Hunt") {
    override fun newRound() = Random.nextInt(0, noteKeys.size)
    override fun optionLabels(d: Int) = noteKeys.map { it.first }
    override fun answerIndex(d: Int) = d
    override fun itemTag(d: Int) = noteKeys[d].first
    fun play() { ui.value.data?.let { TonePlayer.playTone(noteKeys[it].second, 900) } }
}
val baseChords = listOf("Major" to listOf(0,4,7), "Minor" to listOf(0,3,7),
    "Diminished" to listOf(0,3,6), "Augmented" to listOf(0,4,8),
    "Dominant 7th" to listOf(0,4,7,10), "Minor 7th" to listOf(0,3,7,10),
    "Major 7th" to listOf(0,4,7,11), "m7♭5" to listOf(0,3,6,10), "Diminished 7th" to listOf(0,3,6,9))
val advChords = listOf("Add 9" to listOf(0,4,7,14), "9th" to listOf(0,4,7,10,14),
    "Maj 9" to listOf(0,4,7,11,14))
class ChordGameViewModel(app: Application) :
    EarGameViewModel<ChordGameViewModel.D>(app, "best_chord", "Chord Quality") {
    data class D(val root: Int, val pool: List<Pair<String, List<Int>>>, val idx: Int, val order: List<Int>)
    override fun newRound(): D {
        val pool = if (accuracy >= 0.8f && roundsPlayed >= 5) baseChords + advChords else baseChords
        val q = Random.nextInt(pool.size)
        return D(Random.nextInt(0, 12), pool, q, (pool.indices.filter { it != q } + q).shuffled()) }
    override fun optionLabels(d: D) = d.order.map { d.pool[it].first }
    override fun answerIndex(d: D) = d.order.indexOf(d.idx)
    override fun itemTag(d: D) = d.pool[d.idx].first
    fun play() { val d = ui.value.data ?: return
        TonePlayer.playChord(d.pool[d.idx].second.map { TonePlayer.Notes.freqD(d.root + it) }, 1100, Timbre.PLUCK) }
}
val degreeSolfege = arrayOf("Do","Re","Mi","Fa","Sol","La","Ti","Do")
val degreeSargam = arrayOf("Sa","Re","Ga","Ma","Pa","Dha","Ni","Sa")
class DegreeGameViewModel(app: Application) :
    EarGameViewModel<DegreeGameViewModel.D>(app, "best_degree", "Scale Degrees") {
    data class D(val root: Int, val degree: Int, val order: List<Int>)
    override fun newRound(): D { val deg = Random.nextInt(1, 9)
        return D(Random.nextInt(0, 12), deg, ((1..8).filter { it != deg } + deg).shuffled()) }
    override fun optionLabels(d: D) = d.order.map { "$it · ${degreeSolfege[it-1]} / ${degreeSargam[it-1]}" }
    override fun answerIndex(d: D) = d.order.indexOf(d.degree)
    override fun itemTag(d: D) = "degree ${d.degree}"
    fun play() { val d = ui.value.data ?: return
        TonePlayer.playFrequencies(listOf(TonePlayer.Notes.freqD(d.root.toDouble()),
            TonePlayer.Notes.freqD(d.root + d.degree - 1.0)), noteMs = 550) }
}
class RagaMaqamGameViewModel(app: Application) :
    EarGameViewModel<RagaMaqamGameViewModel.D>(app, "best_ragamaqam", "Raga & Maqam Trainer", 8) {
    data class D(val item: PhraseItem, val order: List<String>)
    override fun newRound(): D { val item = ExtraQuestionsPhrases.random()
        return D(item, ((ExtraQuestionsPhrases.map { it.name } - item.name)
            .shuffled().take(3) + item.name).shuffled()) }
    override fun optionLabels(d: D) = d.order
    override fun answerIndex(d: D) = d.order.indexOf(d.item.name)
    override fun itemTag(d: D) = d.item.name
    fun play() { val d = ui.value.data ?: return
        TonePlayer.playFrequencies(d.item.semitones.map { TonePlayer.Notes.freqD(it) }, noteMs = 400, gapMs = 110) }
}
private val ExtraQuestionsPhrases get() = com.example.musictheory.data.ExtraQuestions.phrases
class LightningViewModel(app: Application) : AndroidViewModel(app) {
    data class Ui(val q: Question, val timeLeft: Int = 60, val score: Int = 0,
        val combo: Int = 0, val flash: Boolean? = null, val finished: Boolean = false)
    private val _ui = MutableStateFlow(Ui(QuestionRepo.all.random()))
    val ui: StateFlow<Ui> = _ui.asStateFlow()
    init { viewModelScope.launch {
        while (_ui.value.timeLeft > 0) { delay(1000)
            _ui.update { if (it.finished) it else it.copy(timeLeft = it.timeLeft - 1) }
            if (_ui.value.timeLeft == 0 && !_ui.value.finished) { _ui.update { it.copy(finished = true) }
                val ctx = getApplication<Application>()
                SettingsRepo.saveBest(ctx, "best_lightning", _ui.value.score)
                ProgressRepo.recordGame(ctx, "Lightning Round", _ui.value.score) } } } }
    fun answer(i: Int) { val s = _ui.value
        if (s.flash != null || s.finished) return
        val ok = i == s.q.answer
        _ui.update { it.copy(flash = ok, score = if (ok) it.score + 10 * min(it.combo + 1, 5) else it.score,
            combo = if (ok) it.combo + 1 else 0) }
        viewModelScope.launch { delay(350)
            _ui.update { it.copy(flash = null, q = QuestionRepo.all.random()) } } }
    fun restart() { _ui.value = Ui(QuestionRepo.all.random()) }
}
class PassPlayViewModel(app: Application) : AndroidViewModel(app) {
    data class Ui(val questions: List<Question> = emptyList(), val idx: Int = 0, val chosen: Int? = null,
        val player: Int = 0, val scores: List<Int> = listOf(0, 0), val finished: Boolean = false)
    private val _ui = MutableStateFlow(Ui(QuestionRepo.all.shuffled().take(10)))
    val ui: StateFlow<Ui> = _ui.asStateFlow()
    init { viewModelScope.launch { ui.filter { it.finished }.collect {
        val ctx = getApplication<Application>()
        val total = it.scores[0] + it.scores[1]
        SettingsRepo.saveBest(ctx, "best_passplay", total)
        ProgressRepo.recordGame(ctx, "Pass & Play", total) } } }
    fun choose(i: Int) = _ui.update { s ->
        if (s.chosen != null || s.finished) return@update s
        val ok = i == s.questions[s.idx].answer
        s.copy(chosen = i, scores = if (ok) s.scores.mapIndexed { p, v -> if (p == s.player) v + 10 else v }
            else s.scores) }
    fun next() = _ui.update { s -> when {
        s.finished || s.chosen == null -> s
        s.idx < s.questions.lastIndex -> s.copy(idx = s.idx + 1, chosen = null, player = 1 - s.player)
        else -> s.copy(finished = true) } }
    fun replay() { _ui.value = Ui(QuestionRepo.all.shuffled().take(10)) }
}
