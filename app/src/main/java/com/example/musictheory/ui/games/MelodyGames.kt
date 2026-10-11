package com.example.musictheory.ui.games
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musictheory.audio.Timbre
import com.example.musictheory.audio.TonePlayer
import com.example.musictheory.data.ProgressRepo
import com.example.musictheory.data.SettingsRepo
import com.example.musictheory.ui.common.*
import kotlinx.coroutines.delay
import kotlin.random.Random
private val echoScales = listOf("Major pentatonic" to listOf(0.0,2.0,4.0,7.0,9.0),
    "Major" to listOf(0.0,2.0,4.0,5.0,7.0,9.0,11.0),
    "Natural minor" to listOf(0.0,2.0,3.0,5.0,7.0,8.0,10.0),
    "Hirajoshi" to listOf(0.0,2.0,3.0,7.0,8.0))
@Composable
fun MelodyEchoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var scaleIdx by rememberSaveable { mutableIntStateOf(0) }
    var round by rememberSaveable { mutableIntStateOf(1) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var target by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var input by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var wrong by rememberSaveable { mutableStateOf(false) }
    var recorded by rememberSaveable { mutableStateOf(false) }
    val scale = echoScales[scaleIdx].second
    val semis = remember(scale) { (scale + scale.map { it + 12 }).distinct().sorted().take(12) }
    val freqs = remember(semis) { semis.map { TonePlayer.Notes.freqD(it) } }
    val labels = remember(semis) { semis.mapIndexed { i, _ -> "${i + 1}" } }
    fun playTarget() = TonePlayer.playFrequencies(target.map { freqs[it] }, noteMs = 380, gapMs = 130, timbre = Timbre.PLUCK)
    fun newRound() { val len = round + 2; var i = Random.nextInt(0, semis.size)
        val t = mutableListOf(i); repeat(len - 1) { i = (i + Random.nextInt(-2, 3)).coerceIn(0, semis.size - 1); t.add(i) }
        target = t; input = emptyList(); wrong = false }
    LaunchedEffect(round, scaleIdx) { if (!finished) { if (target.isEmpty()) newRound(); delay(500); playTarget() } }
    LaunchedEffect(input) { if (input.size == target.size && target.isNotEmpty() && !finished) {
        val ok = input == target; if (ok) score += 10 * round else wrong = true
        delay(1400); if (round < 5) { round++; newRound() } else finished = true } }
    LaunchedEffect(finished) { if (finished && !recorded) { recorded = true
        SettingsRepo.saveBest(context, "best_echo", score)
        ProgressRepo.recordGame(context, "Melody Echo", score) } }
    if (finished) { val best by rememberBest("best_echo")
        GameScaffold("Melody Echo", onBack) { ResultPanel("Score: $score",
            "Best: $best — your echo is world-class!") { round = 1; score = 0; recorded = false
            finished = false; target = emptyList(); newRound() } }
        return }
    GameScaffold("Melody Echo", onBack) {
        ScoreRow("Round $round of 5 · ${echoScales[scaleIdx].first}", "Score: $score")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 10.dp)) {
            echoScales.forEachIndexed { i, (nm, _) -> FilterChip(scaleIdx == i, {
                scaleIdx = i; round = 1; score = 0; finished = false; target = emptyList() },
                { Text(nm) }, modifier = Modifier.padding(end = 6.dp)) } }
        Text(when { input.size < target.size && target.isNotEmpty() ->
                "🎧 Echo it back — ${input.size}/${target.size} played"
            wrong -> "Not quite — the phrase was: " + target.joinToString(" ") { "${it + 1}" }
            else -> "Correct! Longer this time…" },
            fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        Row(Modifier.padding(top = 10.dp)) { Button(onClick = { playTarget() }) { Text("▶ Hear phrase again") } }
        WorldKeyboard(freqs = freqs, labels = labels, onKey = { i, f ->
            if (input.size < target.size) { TonePlayer.playTone(f, 500, 0.8, Timbre.PLUCK); input = input + i } },
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
        Text("Keys are numbered within the scale — 1 is the tonic.", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(24.dp)) }
}
private val dictScales = listOf("Major pentatonic" to listOf(0.0,2.0,4.0,7.0,9.0),
    "Major" to listOf(0.0,2.0,4.0,5.0,7.0,9.0,11.0), "Hirajoshi" to listOf(0.0,2.0,3.0,7.0,8.0))
@Composable
fun MelodyDictationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var scaleIdx by rememberSaveable { mutableIntStateOf(0) }
    var round by rememberSaveable { mutableIntStateOf(1) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var target by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var answer by rememberSaveable { mutableStateOf(listOf<Int?>(null, null, null)) }
    var graded by rememberSaveable { mutableStateOf(false) }
    var recorded by rememberSaveable { mutableStateOf(false) }
    val scale = dictScales[scaleIdx].second
    val lengths = listOf(3, 3, 4, 5); val len = lengths[(round - 1).coerceIn(0, 3)]
    val semis = remember(scale) { (scale + scale.map { it + 12 }).distinct().sorted().take(8) }
    val freqs = remember(semis) { semis.map { TonePlayer.Notes.freqD(it) } }
    fun playTarget() = TonePlayer.playFrequencies(target.map { freqs[it] }, noteMs = 380, gapMs = 130, timbre = Timbre.PLUCK)
    fun newRound() { val t = mutableListOf<Int>(); var i = Random.nextInt(0, semis.size); t.add(i)
        repeat(len - 1) { i = (i + Random.nextInt(-2, 3)).coerceIn(0, semis.size - 1); t.add(i) }
        target = t; answer = List(len) { null }; graded = false }
    LaunchedEffect(round, scaleIdx) { if (!finished) { if (target.isEmpty()) newRound(); delay(500); playTarget() } }
    LaunchedEffect(finished) { if (finished && !recorded) { recorded = true
        SettingsRepo.saveBest(context, "best_dictation", score)
        ProgressRepo.recordGame(context, "Melody Dictation", score) } }
    if (finished) { val best by rememberBest("best_dictation")
        GameScaffold("Melody Dictation", onBack) { ResultPanel("Score: $score", "Best: $best — dictation master!") {
            round = 1; score = 0; recorded = false; finished = false; target = emptyList() } }
        return }
    val okC = correctColor()
    GameScaffold("Melody Dictation", onBack) {
        ScoreRow("Round $round of 4 · ${dictScales[scaleIdx].first}", "Score: $score")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp)) {
            dictScales.forEachIndexed { i, (nm, _) -> FilterChip(scaleIdx == i, {
                scaleIdx = i; round = 1; score = 0; finished = false; target = emptyList() },
                { Text(nm) }, modifier = Modifier.padding(end = 6.dp)) } }
        Text(if (graded) "Green = yours & correct · Gold = target" else "Place one note per column, top = high",
            fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
        Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            semis.indices.reversed().forEach { rowIdx ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    (0 until len).forEach { col ->
                        val picked = answer.getOrNull(col) == rowIdx
                        Button(onClick = { if (!graded && col < answer.size) {
                                answer = answer.toMutableList().also { if (it.size > col) it[col] = rowIdx }
                                TonePlayer.playTone(freqs[rowIdx], 400, 0.7, Timbre.PLUCK) } },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = when {
                                graded && col < target.size && target[col] == rowIdx && picked -> okC
                                graded && col < target.size && target[col] == rowIdx -> Color(0xFFF9A825)
                                picked -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant },
                                contentColor = if (picked) Color.White
                                else MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.weight(1f).padding(1.dp).height(40.dp)) {
                            Text(if (picked) "♪" else "", fontSize = 13.sp) } } } } }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            OutlinedButton(onClick = { playTarget() }, modifier = Modifier.weight(1f)) { Text("▶ Phrase") }
            Button(onClick = { val c = target.withIndex().count { (i, t) -> answer.getOrNull(i) == t }
                score += c * 2 + (if (c == len) 5 else 0); graded = true },
                enabled = !graded && answer.filterNotNull().size == len,
                modifier = Modifier.weight(1f).padding(start = 6.dp)) { Text("✓ Grade") } }
        if (graded) { Button(onClick = { if (round < 4) { round++; newRound() } else finished = true },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Text(if (round < 4) "Next phrase" else "See results", fontWeight = FontWeight.Bold) } }
        Spacer(Modifier.height(24.dp)) }
}
