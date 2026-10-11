package com.example.musictheory.ui.games
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musictheory.audio.Timbre
import com.example.musictheory.audio.TonePlayer
import com.example.musictheory.data.KeySigGen
import com.example.musictheory.data.ProgressRepo
import com.example.musictheory.data.SettingsRepo
import com.example.musictheory.data.StaffNote
import com.example.musictheory.ui.common.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.random.Random
private data class ScalePuzzle(val name: String, val semis: List<Int>, val steps: String)
private val puzzles = listOf(
    ScalePuzzle("Major", listOf(0,2,4,5,7,9,11), "W W H W W W H"),
    ScalePuzzle("Natural minor", listOf(0,2,3,5,7,8,10), "W H W W H W W"),
    ScalePuzzle("Dorian", listOf(0,2,3,5,7,9,10), "W H W W W H W"),
    ScalePuzzle("Phrygian", listOf(0,1,3,5,7,8,10), "H W W W H W W"),
    ScalePuzzle("Lydian", listOf(0,2,4,6,7,9,11), "W W W H W W H"),
    ScalePuzzle("Mixolydian", listOf(0,2,4,5,7,9,10), "W W H W W H W"),
    ScalePuzzle("Major pentatonic", listOf(0,2,4,7,9), "W W 1½ W 1½"),
    ScalePuzzle("Minor pentatonic", listOf(0,3,5,7,10), "1½ W W 1½ W"),
    ScalePuzzle("Blues", listOf(0,3,5,6,7,10), "1½ W H H 1½ W"),
    ScalePuzzle("Hirajoshi", listOf(0,2,3,7,8), "W H 2 H 1"))
@Composable
fun ScaleBuilderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var round by rememberSaveable { mutableIntStateOf(1) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var puzzleIdx by rememberSaveable { mutableIntStateOf(Random.nextInt(puzzles.size)) }
    var selected by rememberSaveable { mutableStateOf(listOf(0)) }
    var resultState by rememberSaveable { mutableIntStateOf(-1) }
    var recorded by rememberSaveable { mutableStateOf(false) }
    val puzzle = puzzles[puzzleIdx % puzzles.size]
    val result = when (resultState) { 1 -> true; 0 -> false; else -> null }
    LaunchedEffect(resultState) { if (result != null) { delay(1800)
        if (round < 8) { round++; puzzleIdx = Random.nextInt(puzzles.size)
            selected = listOf(0); resultState = -1 } else finished = true } }
    LaunchedEffect(finished) { if (finished && !recorded) { recorded = true
        SettingsRepo.saveBest(context, "best_scalebuild", score)
        ProgressRepo.recordGame(context, "Scale Builder", score) } }
    if (finished) { val best by rememberBest("best_scalebuild")
        GameScaffold("Scale Builder", onBack) {
            ResultPanel("Score: $score of 80", "Best: $best — scales by construction!") {
                round = 1; score = 0; recorded = false; finished = false
                puzzleIdx = Random.nextInt(puzzles.size); selected = listOf(0); resultState = -1 } }
        return }
    GameScaffold("Scale Builder", onBack) {
        ScoreRow("Round $round of 8", "Score: $score")
        InfoCard("Build ${puzzle.name} from C — steps: ${puzzle.steps}")
        Text("Tonic (C) is given. Tap the other notes, then Check.", fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        KeyPad12(count = 12, selected = selected.toSet(), locked = setOf(0), onToggle = { i ->
            if (result == null) selected = (if (i in selected) selected - i else selected + i).sorted() },
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
        Text("Selected: " + selected.joinToString(" ") { keyLabel(it) }, fontSize = 14.sp,
            modifier = Modifier.padding(top = 10.dp))
        Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Button(onClick = { val ok = selected.toSet() == puzzle.semis.toSet()
                resultState = if (ok) 1 else 0; if (ok) score += 10 },
                enabled = result == null, modifier = Modifier.weight(1f)) { Text("✓ Check") }
            OutlinedButton(onClick = { TonePlayer.playScale(puzzle.semis.map { it.toDouble() }) },
                enabled = result != null, modifier = Modifier.weight(1f).padding(start = 8.dp)) { Text("▶ Hear it") } }
        FeedbackText(when (result) { null -> ""; true -> "Correct — ${puzzle.name} is exactly that!"
            else -> "Not quite. The notes were " + puzzle.semis.joinToString(" ") { keyLabel(it) } }, result)
        Spacer(Modifier.height(24.dp)) }
}
@Composable
fun ChordBuilderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var round by rememberSaveable { mutableIntStateOf(1) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var poolAdv by rememberSaveable { mutableStateOf(false) }
    var root by rememberSaveable { mutableIntStateOf(0) }
    var targetIdx by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var resultState by rememberSaveable { mutableIntStateOf(-1) }
    var recorded by rememberSaveable { mutableStateOf(false) }
    var initialized by rememberSaveable { mutableStateOf(false) }
    val pool = if (poolAdv) baseChords + advChords else baseChords
    val target = pool[targetIdx % pool.size]
    val absTarget = target.second.map { root + it }.toSet()
    val result = when (resultState) { 1 -> true; 0 -> false; else -> null }
    fun deal() { root = Random.nextInt(0, 12)
        targetIdx = Random.nextInt(0, if (poolAdv) baseChords.size + advChords.size else baseChords.size)
        selected = emptyList(); resultState = -1 }
    LaunchedEffect(Unit) { poolAdv = SettingsRepo.bestFlow(context, "best_chord").first() >= 60
        if (!initialized) { deal(); initialized = true } }
    LaunchedEffect(resultState) { if (result != null) { delay(1800)
        if (round < 8) { round++; deal() } else finished = true } }
    LaunchedEffect(finished) { if (finished && !recorded) { recorded = true
        SettingsRepo.saveBest(context, "best_chordbuild", score)
        ProgressRepo.recordGame(context, "Chord Builder", score) } }
    if (finished) { val best by rememberBest("best_chordbuild")
        GameScaffold("Chord Builder", onBack) {
            ResultPanel("Score: $score of 80", "Best: $best — thirds by hand!") {
                round = 1; score = 0; recorded = false; finished = false; deal() } }
        return }
    GameScaffold("Chord Builder", onBack) {
        ScoreRow("Round $round of 8", "Score: $score")
        InfoCard("Build a ${target.first} on ${keyLabel(root)}")
        KeyPad12(count = 26, selected = selected.toSet(), onToggle = { i ->
            if (result == null) selected = (if (i in selected) selected - i else selected + i).sorted() },
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
        Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Button(onClick = { val ok = selected.toSet() == absTarget
                resultState = if (ok) 1 else 0; if (ok) score += 10 },
                enabled = result == null, modifier = Modifier.weight(1f)) { Text("✓ Check") }
            OutlinedButton(onClick = { TonePlayer.playChord(
                absTarget.map { TonePlayer.Notes.freqD(it.toDouble()) }, 1100, Timbre.PLUCK) },
                enabled = result != null, modifier = Modifier.weight(1f).padding(start = 8.dp)) { Text("▶ Hear it") } }
        FeedbackText(when (result) { null -> ""; true -> "Correct — ${keyLabel(root)} ${target.first}!"
            else -> "The notes were " + absTarget.sorted().joinToString(" ") { keyLabel(it) } }, result)
        Spacer(Modifier.height(24.dp)) }
}
@Composable
fun KeySigDrillScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var n by remember { mutableIntStateOf(0) }
    var q by remember { mutableStateOf(KeySigGen.generate(0)) }
    var flash by remember { mutableStateOf<Boolean?>(null) }
    var timeLeft by remember { mutableIntStateOf(60) }
    var score by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    var recorded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (timeLeft > 0) { delay(1000); timeLeft-- }
        if (!finished) finished = true }
    LaunchedEffect(finished) { if (finished && !recorded) { recorded = true
        SettingsRepo.saveBest(context, "best_keysig", score)
        ProgressRepo.recordGame(context, "Key Signature Drill", score) } }
    LaunchedEffect(flash) { if (flash != null) { delay(350); flash = null
        n++; q = KeySigGen.generate(n) } }
    if (finished) { val best by rememberBest("best_keysig")
        GameScaffold("Key Signature Drill", onBack) {
            ResultPanel("Time! $score points", "Best: $best") {
                timeLeft = 60; score = 0; recorded = false; finished = false } }
        return }
    GameScaffold("Key Signature Drill", onBack) {
        ScoreRow("⏱ ${timeLeft}s", "Score: $score")
        InfoCard(q.text); Spacer(Modifier.height(14.dp))
        q.options.forEachIndexed { i, opt ->
            val state = when { flash == null -> OptState.IDLE
                i == q.answer -> OptState.CORRECT; else -> OptState.MUTED }
            OptionButton(opt, state, enabled = flash == null) { if (flash == null) {
                val ok = i == q.answer; flash = ok; if (ok) score += 10 } }
            Spacer(Modifier.height(8.dp)) }
        Spacer(Modifier.height(24.dp)) }
}
@Composable
fun StaffReaderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tier by rememberSaveable { mutableIntStateOf(0) }
    var round by rememberSaveable { mutableIntStateOf(1) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var idx by rememberSaveable { mutableIntStateOf(4) }
    var options by rememberSaveable { mutableStateOf(listOf("D","G","B","E")) }
    var chosenIdx by rememberSaveable { mutableIntStateOf(-1) }
    var recorded by rememberSaveable { mutableStateOf(false) }
    val spec = StaffNote.tiers[tier % StaffNote.tiers.size]
    val correctLetter = StaffNote.name(idx)
    val chosen = if (chosenIdx >= 0) options.getOrNull(chosenIdx) else null
    fun newRound() { idx = spec.range.random(); val c = StaffNote.name(idx)
        options = (("CDEFGAB".map { it.toString() } - c).shuffled().take(3) + c).shuffled()
        chosenIdx = -1 }
    LaunchedEffect(Unit) { if (correctLetter !in options) newRound() }
    LaunchedEffect(chosenIdx) { if (chosenIdx >= 0 && !finished) { delay(1400)
        if (round < 10) { round++; newRound() } else finished = true } }
    LaunchedEffect(finished) { if (finished && !recorded) { recorded = true
        SettingsRepo.saveBest(context, "best_staff", score)
        ProgressRepo.recordGame(context, "Staff Reader", score) } }
    if (finished) { val best by rememberBest("best_staff")
        GameScaffold("Staff Reader", onBack) {
            ResultPanel("Score: $score of 100", "Best: $best") {
                round = 1; score = 0; recorded = false; finished = false; newRound() } }
        return }
    val okCol = correctColor(); val badCol = wrongColor()
    val line = MaterialTheme.colorScheme.onSurface
    GameScaffold("Staff Reader", onBack) {
        ScoreRow("Round $round of 10", "Score: $score")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp)) {
            listOf("Treble", "Treble+led", "Bass", "Bass+led").forEachIndexed { i, nm ->
                FilterChip(tier == i, { tier = i; round = 1; score = 0; finished = false; newRound() },
                    { Text(nm) }, modifier = Modifier.padding(end = 6.dp)) } }
        InfoCard("Which note is this? (${if (spec.bass) "Bass" else "Treble"} clef)")
        Canvas(Modifier.fillMaxWidth().height(190.dp).padding(vertical = 10.dp)) {
            val gap = 26f; val cy = size.height / 2f
            for (k in -2..2) { val y = cy + k * gap
                drawLine(line, Offset(40f, y), Offset(size.width - 40f, y), 3f) }
            fun y(d: Int) = cy - (d - spec.bottomLine) * gap / 2f
            if (!spec.bass) { drawCircle(line, 12f, Offset(90f, y(4)), style = Stroke(4f))
                drawLine(line, Offset(90f, cy - gap * 2.6f), Offset(90f, cy + gap * 2.4f), 4f) }
            else { val fY = y(-4)
                drawArc(line, -60f, 240f, false, style = Stroke(6f),
                    topLeft = Offset(50f, fY - 40f), size = Size(70f, 80f))
                drawCircle(line, 5f, Offset(140f, fY + gap / 2f))
                drawCircle(line, 5f, Offset(140f, fY - gap / 2f)) }
            StaffNote.ledgers(spec, idx).forEach { ld ->
                drawLine(line, Offset(size.width / 2f - 42f, y(ld)),
                    Offset(size.width / 2f + 42f, y(ld)), 3f) }
            drawOval(if (chosen == null) line else if (chosen == correctLetter) okCol else badCol,
                topLeft = Offset(size.width / 2f - 22f, y(idx) - 14f), size = Size(44f, 28f)) }
        Spacer(Modifier.height(6.dp))
        options.forEachIndexed { oi, opt ->
            val state = when { chosen == null -> OptState.IDLE
                opt == correctLetter -> OptState.CORRECT
                oi == chosenIdx -> OptState.WRONG; else -> OptState.MUTED }
            OptionButton(opt, state, enabled = chosen == null) {
                if (chosen == null) { chosenIdx = oi; if (opt == correctLetter) score += 10 } }
            Spacer(Modifier.height(8.dp)) }
        FeedbackText(if (chosen == null) "" else "That's $correctLetter",
            chosen?.let { it == correctLetter })
        Spacer(Modifier.height(24.dp)) }
}
