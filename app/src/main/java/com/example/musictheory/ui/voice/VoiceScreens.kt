package com.example.musictheory.ui.voice
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musictheory.audio.PitchEngine
import com.example.musictheory.audio.TonePlayer
import com.example.musictheory.data.ProgressRepo
import com.example.musictheory.data.SettingsRepo
import com.example.musictheory.ui.common.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
private val NAMES = arrayOf("C","C♯","D","D♯","E","F","F♯","G","G♯","A","A♯","B")
@Composable
fun TunerScreen(onBack: () -> Unit) {
    MicGate { val hz by PitchEngine.hz.collectAsState()
        DisposableEffect(Unit) { PitchEngine.start(LocalContext.current); onDispose { PitchEngine.stop() } }
        val semis = hz?.let { PitchEngine.semisOf(it) }
        val nearest = semis?.let { it.roundToInt() }
        val cents = semis?.let { (it - it.roundToInt()) * 100.0 } ?: 0.0
        val inTune = hz != null && abs(cents) <= 8
        GameScaffold("🎤 Tuner", onBack) {
            Column(Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (hz == null) "Listen…"
                    else NAMES[((nearest ?: 0) % 12 + 12) % 12] + (4 + ((nearest ?: 0) / 12)),
                    fontSize = 56.sp, fontWeight = FontWeight.Bold,
                    color = if (inTune) correctColor() else MaterialTheme.colorScheme.primary)
                Text(if (hz == null) "—" else "%.1f Hz".format(hz), fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (hz == null) "" else when { inTune -> "In tune ✓"; cents > 0 ->
                    "♯ %.0f cents sharp".format(cents); else -> "♭ %.0f cents flat".format(-cents) },
                    fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                Canvas(Modifier.fillMaxWidth().height(120.dp).padding(top = 8.dp)) {
                    val cx = size.width / 2f; val cy = size.height * 0.95f; val r = size.width * 0.42f
                    drawArc(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), 180f, 180f, false, style = Stroke(4f))
                    val ang = Math.toRadians(180.0 + ((cents / 50.0).coerceIn(-1.0, 1.0) + 1.0) * 90.0)
                    drawLine(if (inTune) correctColor() else MaterialTheme.colorScheme.tertiary,
                        Offset(cx, cy), Offset(cx + cos(ang).toFloat() * r, cy + sin(ang).toFloat() * r),
                        6f, StrokeCap.Round) }
                Text("◀ flat · in tune · sharp ▶", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text("Reference tone", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp)) {
                NAMES.forEachIndexed { i, nm ->
                    FilterChip(false, { TonePlayer.playTone(TonePlayer.Notes.freqD(i), 1200) },
                        { Text(nm) }, modifier = Modifier.padding(end = 4.dp)) } }
            Spacer(Modifier.height(24.dp)) } }
}
@Composable
fun MatchDroneScreen(onBack: () -> Unit) {
    MicGate { val context = LocalContext.current
        var round by rememberSaveable { mutableIntStateOf(1) }
        var score by rememberSaveable { mutableIntStateOf(0) }
        var finished by rememberSaveable { mutableStateOf(false) }
        var root by rememberSaveable { mutableIntStateOf(0) }
        var phase by rememberSaveable { mutableIntStateOf(0) }
        var verdict by rememberSaveable { mutableStateOf("") }
        var recorded by rememberSaveable { mutableStateOf(false) }
        DisposableEffect(Unit) { onDispose { TonePlayer.stopAll(); PitchEngine.stop() } }
        LaunchedEffect(round) { if (finished) return@LaunchedEffect
            if (round == 1) root = Random.nextInt(0, 12)
            phase = 0
            repeat(3) { TonePlayer.playChord(listOf(TonePlayer.Notes.freqD(root - 12.0),
                TonePlayer.Notes.freqD(root - 5.0)), 3200, 0.2); delay(3300) }
            phase = 1; PitchEngine.start(context)
            val cents = mutableListOf<Double>()
            val collector = launch { PitchEngine.hz.collect { h ->
                h?.let { cents += PitchEngine.centsFrom(root.toDouble(), it) } } }
            delay(3000); collector.cancel(); PitchEngine.stop()
            val sorted = cents.sorted(); val median = sorted.getOrNull(cents.size / 2)
            val dev = cents.map { abs(it - (median ?: 0.0)) }.sorted()
                .getOrNull(cents.size / 2) ?: 999.0
            val pts = when { median == null -> 0
                abs(median) <= 25 && dev <= 40 -> 10; abs(median) <= 60 -> 5; else -> 0 }
            score += pts
            verdict = when { median == null -> "No steady pitch heard — hold the note longer."
                pts == 10 -> "Locked in — %.0f cents off, rock steady!".format(median)
                pts == 5 -> "Close — %.0f cents off. Let the drone be your centre.".format(median)
                else -> "%.0f cents off — find the drone's home.".format(median ?: 0.0) }
            phase = 2 }
        LaunchedEffect(finished) { if (finished && !recorded) { recorded = true
            SettingsRepo.saveBest(context, "best_drone", score)
            ProgressRepo.recordGame(context, "Match the Drone", score) } }
        if (finished) { val best by rememberBest("best_drone")
            GameScaffold("Match the Drone", onBack) { ResultPanel("Score: $score of 60",
                "Best: $best — sa straight from the heart.") { round = 1; score = 0; recorded = false
                finished = false; root = Random.nextInt(0, 12) } }; return }
        GameScaffold("Match the Drone", onBack) {
            ScoreRow("Round $round of 6", "Score: $score")
            InfoCard("Sing the Sa (${TonePlayer.Notes.name(root)}) and hold it steady")
            when (phase) {
                0 -> Text("🎧 Listen to the drone…", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp))
                1 -> Text("🎵 Sing and HOLD…", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp))
                2 -> Column(Modifier.padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(verdict, fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { if (round < 6) { round++; root = Random.nextInt(0, 12) }
                        else finished = true }) { Text(if (round < 6) "Next drone" else "See results") } } } } }
}
private val singIntervals = listOf("Major 2nd" to 2, "Major 3rd" to 4, "Perfect 4th" to 5,
    "Perfect 5th" to 7, "Major 6th" to 9, "Octave" to 12)
@Composable
fun SingIntervalScreen(onBack: () -> Unit) {
    MicGate { val context = LocalContext.current
        var round by rememberSaveable { mutableIntStateOf(1) }
        var score by rememberSaveable { mutableIntStateOf(0) }
        var finished by rememberSaveable { mutableStateOf(false) }
        var root by rememberSaveable { mutableIntStateOf(0) }
        var iIdx by rememberSaveable { mutableIntStateOf(0) }
        var phase by rememberSaveable { mutableIntStateOf(0) }
        var verdict by rememberSaveable { mutableStateOf("") }
        var recorded by rememberSaveable { mutableStateOf(false) }
        DisposableEffect(Unit) { onDispose { TonePlayer.stopAll(); PitchEngine.stop() } }
        LaunchedEffect(round) { if (finished) return@LaunchedEffect
            root = Random.nextInt(0, 12); iIdx = Random.nextInt(singIntervals.size); phase = 0
            TonePlayer.playTone(TonePlayer.Notes.freqD(root.toDouble()), 700)
            delay(900); TonePlayer.playTone(TonePlayer.Notes.freqD(root + singIntervals[iIdx].second), 900)
            delay(1100); phase = 1; PitchEngine.start(context)
            val cents = mutableListOf<Double>()
            val collector = launch { PitchEngine.hz.collect { h -> h?.let {
                cents += PitchEngine.centsFrom((root + singIntervals[iIdx].second).toDouble(), it) } } }
            delay(3000); collector.cancel(); PitchEngine.stop()
            val median = cents.sorted().getOrNull(cents.size / 2)
            val pts = when { median == null -> 0; abs(median) <= 30 -> 10; abs(median) <= 70 -> 5; else -> 0 }
            score += pts
            verdict = when { median == null -> "No steady pitch heard — sing longer."
                pts == 10 -> "Nailed it — %.0f cents from the ${singIntervals[iIdx].first}!".format(median)
                pts == 5 -> "Close — %.0f cents from the ${singIntervals[iIdx].first}.".format(median)
                else -> "The target was the ${singIntervals[iIdx].first}." }
            phase = 2 }
        LaunchedEffect(finished) { if (finished && !recorded) { recorded = true
            SettingsRepo.saveBest(context, "best_sing", score)
            ProgressRepo.recordGame(context, "Sing the Interval", score) } }
        if (finished) { val best by rememberBest("best_sing")
            GameScaffold("Sing the Interval", onBack) { ResultPanel("Score: $score of 80", "Best: $best") {
                round = 1; score = 0; recorded = false; finished = false } }; return }
        GameScaffold("Sing the Interval", onBack) {
            ScoreRow("Round $round of 8", "Score: $score")
            InfoCard("Tonic plays, then the target. Sing the TARGET back and hold.")
            when (phase) {
                0 -> Text("🎧 Listen…", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp))
                1 -> Text("🎵 Sing the target…", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp))
                2 -> Column(Modifier.padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(verdict, fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { if (round < 8) round++ else finished = true }) {
                        Text(if (round < 8) "Next" else "See results") } } } } }
}
