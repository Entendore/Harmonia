package com.example.musictheory.vm
import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musictheory.audio.TonePlayer
import com.example.musictheory.data.ProgressRepo
import com.example.musictheory.data.RhythmPatterns
import com.example.musictheory.data.SettingsRepo
import com.example.musictheory.util.RhythmGrid
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random
abstract class RhythmGameViewModel(app: Application, private val bestKey: String,
    private val gameName: String, private val accGame: String) : AndroidViewModel(app) {
    data class Ui(val round: Int = 1, val totalScore: Int = 0, val finished: Boolean = false,
        val phase: Int = 0, val patternIdx: Int = -1, val hits: Int = 0, val maxHits: Int = 0,
        val tolMs: Long = 150, val note: String = "")
    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui.asStateFlow()
    private val taps = mutableListOf<Long>()
    private var job: Job? = null
    private var base = 0L
    init { viewModelScope.launch {
        val acc = ProgressRepo.accuracy(getApplication(), accGame).first()
        val tol = when { acc >= 0.85f -> 95L; acc >= 0.6f -> 120L; else -> 150L }
        _ui.update { it.copy(tolMs = tol) }; startRound(1, 0) } }
    protected abstract fun click(accent: Boolean)
    protected abstract fun listenAccent(): Boolean
    fun startRound(round: Int, score: Int) { job?.cancel(); taps.clear()
        val pi = Random.nextInt(RhythmPatterns.all.size); val p = RhythmPatterns.all[pi]
        _ui.value = Ui(round, score, false, 0, pi, 0, 0, _ui.value.tolMs, p.note)
        val tol = _ui.value.tolMs
        job = viewModelScope.launch {
            fun clickFor(i: Int) = click(listenAccent() && ((i % p.slots) in p.accents))
            RhythmGrid.run(p.slots, 2, p.slotMs, SystemClock.elapsedRealtime(),
                { SystemClock.elapsedRealtime() }, { delay(it) }) { clickFor(it) }
            delay(600); _ui.update { it.copy(phase = 1) }
            base = SystemClock.elapsedRealtime()
            RhythmGrid.run(p.slots, 2, p.slotMs, base,
                { SystemClock.elapsedRealtime() }, { delay(it) }) { clickFor(it) }
            val expected = p.accents.flatMap { a ->
                listOf(a * p.slotMs, a * p.slotMs + p.slots * p.slotMs) }
            val used = mutableSetOf<Long>(); var hits = 0
            for (t in taps.toList()) { val rel = t - base
                val nearest = expected.minBy { abs(rel - it) }
                if (abs(rel - nearest) <= tol && used.add(nearest)) hits++ }
            _ui.update { it.copy(phase = 2, hits = hits, maxHits = p.accents.size * 2,
                totalScore = it.totalScore + hits * 10) } } }
    fun tap() { if (_ui.value.phase == 1) taps.add(SystemClock.elapsedRealtime()) }
    fun next() { val s = _ui.value; if (s.phase != 2) return
        if (s.round < 5) startRound(s.round + 1, s.totalScore)
        else viewModelScope.launch { _ui.update { it.copy(finished = true) }
            val ctx = getApplication<Application>()
            SettingsRepo.saveBest(ctx, bestKey, _ui.value.totalScore)
            ProgressRepo.recordGame(ctx, gameName, _ui.value.totalScore) } }
    fun restart() { viewModelScope.launch { _ui.update { it.copy(finished = false) }; startRound(1, 0) } }
    fun stop() { job?.cancel(); TonePlayer.stopAll() }
}
class RhythmTapViewModel(app: Application) :
    RhythmGameViewModel(app, "best_rhythm", "Rhythm Tap", "Rhythm Tap") {
    override fun click(accent: Boolean) = if (accent) TonePlayer.playTone(1320.0, 80, volume = 0.9)
        else TonePlayer.playTone(660.0, 55, volume = 0.45)
    override fun listenAccent() = true
}
class RhythmReadViewModel(app: Application) :
    RhythmGameViewModel(app, "best_rhythmread", "Rhythm Reading", "Rhythm Reading") {
    override fun click(accent: Boolean) = TonePlayer.playTone(660.0, 55, 0.45)
    override fun listenAccent() = false
}
