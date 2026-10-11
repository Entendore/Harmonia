package com.example.musictheory.ui.games
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musictheory.audio.Timbre
import com.example.musictheory.audio.TonePlayer
import com.example.musictheory.data.CompareData
import com.example.musictheory.data.ProgressRepo
import com.example.musictheory.data.SettingsRepo
import com.example.musictheory.ui.common.*
import com.example.musictheory.vm.DegreeGameViewModel
import com.example.musictheory.vm.LightningViewModel
import com.example.musictheory.vm.PassPlayViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random
@Composable
fun ScaleDegreeScreen(onBack: () -> Unit) {
    val vm: DegreeGameViewModel = viewModel(); val ui by vm.ui.collectAsState()
    LaunchedEffect(ui.data, ui.round) { if (ui.data != null && ui.chosen == null && !ui.finished) vm.play() }
    if (ui.finished) { val best by rememberBest("best_degree")
        GameScaffold("Scale Degrees", onBack) { ResultPanel("You scored ${ui.score} of ${vm.totalRounds * 10}",
            "Best: $best") { vm.restart() } }; return }
    val d = ui.data
    GameScaffold("Scale Degrees", onBack) {
        ScoreRow("Round ${ui.round} of ${vm.totalRounds}", "Score: ${ui.score}")
        InfoCard("The tonic plays, then a second note. Which degree of the major scale?")
        Row(Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Button(onClick = { vm.play() }, modifier = Modifier.weight(1f)) { Text("Play") }
            OutlinedButton(onClick = { if (ui.chosen == null) vm.play() },
                modifier = Modifier.weight(1f).padding(start = 8.dp)) { Text("Replay") } }
        Spacer(Modifier.height(10.dp))
        ui.options.forEachIndexed { i, label ->
            val state = when { ui.chosen == null -> OptState.IDLE
                i == ui.answerIndex -> OptState.CORRECT; i == ui.chosen -> OptState.WRONG; else -> OptState.MUTED }
            OptionButton(label, state, enabled = ui.chosen == null) { vm.choose(i) }
            Spacer(Modifier.height(8.dp)) }
        FeedbackText(if (ui.chosen == null) ""
            else "It was degree ${d?.degree} of ${TonePlayer.Notes.name(d?.root ?: 0)} major",
            ui.chosen?.let { it == ui.answerIndex })
        Spacer(Modifier.height(24.dp)) }
}
@Composable
fun LightningScreen(onBack: () -> Unit) {
    val vm: LightningViewModel = viewModel(); val ui by vm.ui.collectAsState()
    if (ui.finished) { val best by rememberBest("best_lightning")
        GameScaffold("Lightning Round", onBack) { ResultPanel("Time! ${ui.score} points",
            "Best: $best — combo multiplier up to ×5") { vm.restart() } }; return }
    GameScaffold("Lightning Round", onBack) {
        ScoreRow("⏱ ${ui.timeLeft}s", "Score: ${ui.score}")
        if (ui.combo > 1) Text("🔥 Combo ×${minOf(ui.combo, 5)}", fontSize = 16.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(top = 4.dp))
        InfoCard(ui.q.text); Spacer(Modifier.height(14.dp))
        ui.q.options.forEachIndexed { i, opt ->
            val state = when { ui.flash == null -> OptState.IDLE
                i == ui.q.answer -> OptState.CORRECT; else -> OptState.MUTED }
            OptionButton(opt, state, enabled = ui.flash == null) { vm.answer(i) }
            Spacer(Modifier.height(8.dp)) }
        Spacer(Modifier.height(24.dp)) }
}
@Composable
fun PassPlayScreen(onBack: () -> Unit) {
    val vm: PassPlayViewModel = viewModel(); val ui by vm.ui.collectAsState()
    if (ui.finished) { val verdict = when { ui.scores[0] > ui.scores[1] -> "Player 1 wins! 🏆"
            ui.scores[1] > ui.scores[0] -> "Player 2 wins! 🏆"; else -> "A perfect tie!" }
        GameScaffold("Pass & Play", onBack) {
            ResultPanel(verdict, "P1: ${ui.scores[0]}  ·  P2: ${ui.scores[1]}") { vm.replay() } }
        return }
    val q = ui.questions[ui.idx]
    GameScaffold("Pass & Play", onBack) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..1).forEach { p -> Card(shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (p == ui.player)
                    MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.weight(1f)) {
                    Text("P${p + 1}  ${ui.scores[p]}", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)) } } }
        Text("Question ${ui.idx + 1} of ${ui.questions.size} — Player ${ui.player + 1}'s turn",
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
        InfoCard(q.text); Spacer(Modifier.height(14.dp))
        q.options.forEachIndexed { i, opt ->
            val state = when { ui.chosen == null -> OptState.IDLE
                i == q.answer -> OptState.CORRECT; i == ui.chosen -> OptState.WRONG; else -> OptState.MUTED }
            OptionButton(opt, state, enabled = ui.chosen == null) { vm.choose(i) }
            Spacer(Modifier.height(8.dp)) }
        if (ui.chosen != null) { Text(q.explanation, fontSize = 14.sp, lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            Button(onClick = { vm.next() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(14.dp)) { Text("Hand over →", fontWeight = FontWeight.Bold) } }
        Spacer(Modifier.height(24.dp)) }
}
@Composable
fun CompareModeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var round by rememberSaveable { mutableIntStateOf(1) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var pairIdx by rememberSaveable { mutableIntStateOf(Random.nextInt(CompareData.contrasts.size)) }
    var targetFirst by rememberSaveable { mutableStateOf(true) }
    var root by rememberSaveable { mutableIntStateOf(0) }
    var chosen by rememberSaveable { mutableIntStateOf(-1) }
    var dealt by rememberSaveable { mutableStateOf(false) }
    var replayKey by rememberSaveable { mutableIntStateOf(0) }
    var recorded by rememberSaveable { mutableStateOf(false) }
    val pair = CompareData.contrasts[pairIdx % CompareData.contrasts.size]
    fun at(pos: Int): List<Double> { val isT = (pos == 0) == targetFirst
        val s = if (isT) pair.targetSemis else pair.otherSemis
        return CompareData.contour.map { TonePlayer.Notes.freqD(root + s[it.coerceAtMost(s.lastIndex)]) } }
    fun deal() { pairIdx = Random.nextInt(CompareData.contrasts.size); targetFirst = Random.nextBoolean()
        root = CompareData.roots.random(); chosen = -1 }
    LaunchedEffect(round) { if (!finished && !dealt) { deal(); dealt = true } }
    LaunchedEffect(round, replayKey) { if (!finished && dealt) { delay(400)
        TonePlayer.playFrequencies(at(0), noteMs = 340, gapMs = 110)
        delay(CompareData.contour.size * 450L + 700)
        TonePlayer.playFrequencies(at(1), noteMs = 340, gapMs = 110) } }
    LaunchedEffect(chosen) { if (chosen != -1 && !finished) { delay(2200)
        if (round < 8) { round++; dealt = false } else finished = true } }
    LaunchedEffect(finished) { if (finished && !recorded) { recorded = true
        SettingsRepo.saveBest(context, "best_compare", score)
        ProgressRepo.recordGame(context, "Compare Mode", score) } }
    if (finished) { val best by rememberBest("best_compare")
        GameScaffold("Compare Mode", onBack) { ResultPanel("Score: $score of 80",
            "Best: $best — you hear the difference.") { round = 1; score = 0; recorded = false
            finished = false; dealt = false } }
        return }
    fun optState(pos: Int) = when { chosen == -1 -> OptState.IDLE
        pos == if (targetFirst) 0 else 1 -> OptState.CORRECT; pos == chosen -> OptState.WRONG; else -> OptState.MUTED }
    GameScaffold("Compare Mode", onBack) {
        ScoreRow("Round $round of 8", "Score: $score")
        InfoCard("The same tune, two modes. Which was “${pair.target}”?"); Spacer(Modifier.height(10.dp))
        Button(onClick = { if (chosen == -1) replayKey++ }, enabled = chosen == -1,
            modifier = Modifier.fillMaxWidth()) { Text("▶ Hear both") }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            OptionButton("🅰 First", optState(0), enabled = chosen == -1,
                modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                if (chosen == -1) { chosen = 0; if (targetFirst) score += 10 } }
            OptionButton("🅱 Second", optState(1), enabled = chosen == -1,
                modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                if (chosen == -1) { chosen = 1; if (!targetFirst) score += 10 } } }
        val f = if (targetFirst) pair.target else pair.other
        val s2 = if (targetFirst) pair.other else pair.target
        FeedbackText(if (chosen == -1) "" else "First was $f · Second was $s2",
            chosen.takeIf { it != -1 }?.let { it == if (targetFirst) 0 else 1 })
        Spacer(Modifier.height(24.dp)) }
}
private val progPairs = listOf(
    ("I–V–vi–IV" to listOf(0,7,9,5)) to ("I–vi–IV–V" to listOf(0,9,5,7)),
    ("I–IV–V–I" to listOf(0,5,7,0)) to ("I–V–IV–I" to listOf(0,7,5,0)),
    ("ii–V–I" to listOf(2,7,0)) to ("I–vi–ii–V" to listOf(0,9,2,7)))
@Composable
fun ProgressionEarScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var round by rememberSaveable { mutableIntStateOf(1) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var pairIdx by rememberSaveable { mutableIntStateOf(0) }
    var aFirst by rememberSaveable { mutableStateOf(true) }
    var key by rememberSaveable { mutableIntStateOf(0) }
    var chosen by rememberSaveable { mutableIntStateOf(-1) }
    var recorded by rememberSaveable { mutableStateOf(false) }
    var replayKey by rememberSaveable { mutableIntStateOf(0) }
    fun play(prog: List<Int>) = TonePlayer.playChordSequence(
        prog.map { d -> listOf(0, 4, 7).map { TonePlayer.Notes.freqD((key + d).toDouble()) } },
        chordMs = 800, gapMs = 150, timbre = Timbre.PLUCK)
    LaunchedEffect(round, replayKey) { if (finished) return@LaunchedEffect
        pairIdx = Random.nextInt(progPairs.size); aFirst = Random.nextBoolean()
        key = Random.nextInt(0, 12); chosen = -1
        delay(500); play(if (aFirst) progPairs[pairIdx].first.second else progPairs[pairIdx].second.second)
        delay(4200); play(if (aFirst) progPairs[pairIdx].second.second else progPairs[pairIdx].first.second) }
    LaunchedEffect(finished) { if (finished && !recorded) { recorded = true
        SettingsRepo.saveBest(context, "best_prog", score)
        ProgressRepo.recordGame(context, "Progression Ear", score) } }
    if (finished) { val best by rememberBest("best_prog")
        GameScaffold("Progression Ear", onBack) { ResultPanel("Score: $score of 80", "Best: $best") {
            round = 1; score = 0; recorded = false; finished = false } }
        return }
    val pr = progPairs[pairIdx]; val correctPos = if (aFirst) 0 else 1
    val first = if (aFirst) pr.first.first else pr.second.first
    val second = if (aFirst) pr.second.first else pr.first.first
    GameScaffold("Progression Ear", onBack) {
        ScoreRow("Round $round of 8", "Score: $score")
        InfoCard("Two loops in ${TonePlayer.Notes.name(key)}. Which is which?")
        Button(onClick = { if (chosen == -1) replayKey++ }, enabled = chosen == -1,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("▶ Replay both") }
        Spacer(Modifier.height(10.dp))
        listOf(first to 0, second to 1).forEach { (label, pos) ->
            val state = when { chosen == -1 -> OptState.IDLE
                pos == correctPos -> OptState.CORRECT; pos == chosen -> OptState.WRONG; else -> OptState.MUTED }
            OptionButton(label, state, enabled = chosen == -1) {
                if (chosen == -1) { chosen = pos; if (pos == correctPos) score += 10 } }
            Spacer(Modifier.height(8.dp)) }
        FeedbackText(if (chosen == -1) "" else "First was $first · Second was $second",
            chosen.takeIf { it != -1 }?.let { it == correctPos })
        Spacer(Modifier.height(24.dp)) }
}
private val toneTargets = listOf("Root" to 0, "Fifth" to 7, "Major 3rd" to 4, "Minor 7th" to 10)
@Composable
fun ChordTargetScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var round by rememberSaveable { mutableIntStateOf(1) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var root by rememberSaveable { mutableIntStateOf(0) }
    var tIdx by rememberSaveable { mutableIntStateOf(0) }
    var chosen by rememberSaveable { mutableIntStateOf(-1) }
    var recorded by rememberSaveable { mutableStateOf(false) }
    fun drone() = TonePlayer.playChord(listOf(TonePlayer.Notes.freqD(root - 12.0),
        TonePlayer.Notes.freqD(root - 5.0)), 2400, 0.18)
    LaunchedEffect(round) { if (finished) return@LaunchedEffect
        root = Random.nextInt(0, 12); tIdx = Random.nextInt(toneTargets.size); chosen = -1
        drone(); delay(500); TonePlayer.playTone(TonePlayer.Notes.freqD(root + toneTargets[tIdx].second), 900) }
    LaunchedEffect(finished) { if (finished && !recorded) { recorded = true
        SettingsRepo.saveBest(context, "best_chordtone", score)
        ProgressRepo.recordGame(context, "Chord Tone Target", score) } }
    LaunchedEffect(chosen) { if (chosen != -1 && !finished) { delay(1800)
        if (round < 10) round++ else finished = true } }
    if (finished) { val best by rememberBest("best_chordtone")
        GameScaffold("Chord Tone Target", onBack) { ResultPanel("Score: $score of 100", "Best: $best") {
            round = 1; score = 0; recorded = false; finished = false } }
        return }
    GameScaffold("Chord Tone Target", onBack) {
        ScoreRow("Round $round of 10", "Score: $score")
        InfoCard("Over the drone, which tone is sounding?")
        Button(onClick = { drone() }, modifier = Modifier.padding(top = 10.dp)) { Text("▶ Replay drone") }
        Spacer(Modifier.height(10.dp))
        toneTargets.forEachIndexed { i, (nm, _) ->
            val state = when { chosen == -1 -> OptState.IDLE; i == tIdx -> OptState.CORRECT
                i == chosen -> OptState.WRONG; else -> OptState.MUTED }
            OptionButton(nm, state, enabled = chosen == -1) {
                if (chosen == -1) { chosen = i; if (i == tIdx) score += 10 } }
            Spacer(Modifier.height(8.dp)) }
        Spacer(Modifier.height(24.dp)) }
}
@Composable
fun SteadinessScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var bpm by rememberSaveable { mutableIntStateOf(100) }
    var phase by rememberSaveable { mutableIntStateOf(0) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var avgBpm by rememberSaveable { mutableIntStateOf(0) }
    val taps = remember { mutableListOf<Long>() }
    val scope = rememberCoroutineScope()
    fun start() { taps.clear(); phase = 1; score = 0; avgBpm = 0
        scope.launch {
            val slot = 60000L / bpm
            repeat(4) { TonePlayer.playTone(1320.0, 80, 0.9, Timbre.BELL); delay(slot) }
            phase = 2
            TonePlayer.playTone(880.0, 50, 0.5); delay(8000)
            phase = 3
            val iois = taps.sorted().zipWithNext { a, b -> b - a }
            if (iois.size >= 3) { val mean = iois.average()
                val sd = kotlin.math.sqrt(iois.map { (it - mean) * (it - mean) }.average())
                score = (100 - sd).roundToInt().coerceIn(0, 100)
                avgBpm = (60000.0 / mean).roundToInt()
                SettingsRepo.saveBest(context, "best_steady", score)
                ProgressRepo.recordGame(context, "Steadiness", score) } } }
    if (phase == 3) { val best by rememberBest("best_steady")
        GameScaffold("Steadiness", onBack) { ResultPanel("Steadiness: $score / 100",
            "Your pace: $avgBpm BPM · Best: $best — " + when { score >= 90 -> "Machine. 🤖"
                score >= 70 -> "Solid and human."; else -> "wonderfully human." }) { phase = 0 } }
        return }
    GameScaffold("Steadiness", onBack) {
        ScoreRow(if (phase == 2) "TAP NOW" else "Target: $bpm BPM", "Score: $score")
        if (phase == 0) { Slider(bpm.toFloat(), { bpm = it.toInt() }, valueRange = 40f..200f,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
            Button(onClick = { start() }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                Text("▶ Start — 4 count-in clicks, then tap in time for 8 s", fontWeight = FontWeight.Bold) } }
        else Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
            Button(onClick = { if (phase == 2) taps.add(SystemClock.elapsedRealtime()) },
                shape = CircleShape, modifier = Modifier.size(190.dp)) {
                Text(if (phase == 1) "…" else "TAP", fontSize = 34.sp, fontWeight = FontWeight.Bold) } }
        Spacer(Modifier.height(24.dp)) }
}
