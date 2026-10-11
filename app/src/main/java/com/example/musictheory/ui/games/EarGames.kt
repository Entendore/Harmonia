package com.example.musictheory.ui.games
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musictheory.audio.TonePlayer
import com.example.musictheory.ui.common.*
import com.example.musictheory.vm.*
@Composable
fun IntervalGameScreen(onBack: () -> Unit) {
    val vm: IntervalGameViewModel = viewModel(); val ui by vm.ui.collectAsState()
    LaunchedEffect(ui.data, ui.round) {
        if (ui.data != null && ui.chosen == null && !ui.finished) vm.play() }
    if (ui.finished) { val best by rememberBest("best_interval")
        GameScaffold("Interval Ear Training", onBack) {
            ResultPanel("You got ${ui.score} of ${vm.totalRounds}", "Best: $best correct") { vm.restart() } }
        return }
    val d = ui.data
    GameScaffold("Interval Ear Training", onBack) {
        ScoreRow("Round ${ui.round} of ${vm.totalRounds}", "Correct: ${ui.score}")
        InfoCard("Which interval did you hear?")
        Row(Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Button(onClick = { vm.play() }, modifier = Modifier.weight(1f)) { Text("Play") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { if (ui.chosen == null) vm.play() },
                modifier = Modifier.weight(1f)) { Text("Replay") } }
        Spacer(Modifier.height(10.dp))
        ui.options.chunked(4).forEachIndexed { r, row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                row.forEachIndexed { c, label -> val i = r * 4 + c
                    val state = when { ui.chosen == null -> OptState.IDLE
                        i == ui.answerIndex -> OptState.CORRECT
                        i == ui.chosen -> OptState.WRONG; else -> OptState.MUTED }
                    OptionButton(label, state, enabled = ui.chosen == null,
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)) { vm.choose(i) } } } }
        val root = d?.root ?: 0
        val semi = d?.pool?.getOrNull(ui.answerIndex)?.second ?: 0
        val from = TonePlayer.Notes.name(root); val to = TonePlayer.Notes.name(root + semi)
        FeedbackText(if (ui.chosen == null) ""
            else if (ui.chosen == ui.answerIndex) "Correct! $from → $to = ${ui.options[ui.answerIndex]}"
            else "It was a ${ui.options[ui.answerIndex]} ($from → $to)",
            ui.chosen?.let { it == ui.answerIndex })
        Spacer(Modifier.height(24.dp)) }
}
@Composable
fun NoteHuntScreen(onBack: () -> Unit) {
    val vm: NoteGameViewModel = viewModel(); val ui by vm.ui.collectAsState()
    LaunchedEffect(ui.data, ui.round) {
        if (ui.data != null && ui.chosen == null && !ui.finished) vm.play() }
    if (ui.finished) { val best by rememberBest("best_note")
        GameScaffold("Note Hunt", onBack) {
            ResultPanel("Final streak: ${ui.score}",
                (if (ui.score >= 8) "Golden ear! 🏆  " else "Keep training!  ") + "Best: $best") { vm.restart() } }
        return }
    GameScaffold("Note Hunt", onBack) {
        ScoreRow("Round ${ui.round} of ${vm.totalRounds}", "Streak: ${ui.score}")
        Text("Listen, then tap the key that matches the note", fontSize = 20.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
        Box(Modifier.fillMaxWidth().padding(top = 14.dp), contentAlignment = Alignment.Center) {
            Button(onClick = { if (ui.chosen == null) vm.play() }) { Text("Play note") } }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 18.dp)) {
            noteKeys.forEachIndexed { i, k ->
                val bg = when { ui.chosen != null && i == ui.answerIndex -> correctColor()
                    ui.chosen == i -> wrongColor(); else -> Color.White }
                Button(onClick = { vm.choose(i) }, shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = bg,
                        contentColor = if (ui.chosen != null) Color.White else Color.Black),
                    modifier = Modifier.width(52.dp).height(112.dp).padding(horizontal = 1.dp)) {
                    Text(if (ui.chosen != null) k.first.removeSuffix("4") else "", fontSize = 16.sp) } } }
        FeedbackText(if (ui.chosen == null) ""
            else if (ui.chosen == ui.answerIndex) "Correct — that was ${noteKeys[ui.answerIndex].first}!"
            else "That was ${noteKeys[ui.chosen?.coerceIn(0, 6) ?: 0].first} — the note was ${noteKeys[ui.answerIndex].first}",
            ui.chosen?.let { it == ui.answerIndex }) }
}
@Composable
fun RagaMaqamScreen(onBack: () -> Unit) {
    val vm: RagaMaqamGameViewModel = viewModel(); val ui by vm.ui.collectAsState()
    LaunchedEffect(ui.data, ui.round) {
        if (ui.data != null && ui.chosen == null && !ui.finished) vm.play() }
    if (ui.finished) { val best by rememberBest("best_ragamaqam")
        GameScaffold("Raga & Maqam Trainer", onBack) {
            ResultPanel("You named ${ui.score / 10} of ${vm.totalRounds} phrases",
                "Best: $best — quarter tones and all!") { vm.restart() } }
        return }
    val d = ui.data
    GameScaffold("Raga & Maqam Trainer", onBack) {
        ScoreRow("Round ${ui.round} of ${vm.totalRounds}", "Score: ${ui.score}")
        InfoCard("Listen — is it a Raga (India) or a Maqam (Arab world)?")
        Row(Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Button(onClick = { vm.play() }, modifier = Modifier.weight(1f)) { Text("Play phrase") }
            OutlinedButton(onClick = { if (ui.chosen == null) vm.play() },
                modifier = Modifier.weight(1f).padding(start = 8.dp)) { Text("Replay") } }
        Spacer(Modifier.height(10.dp))
        ui.options.forEach { opt ->
            val state = when { ui.chosen == null -> OptState.IDLE
                opt == (d?.item?.name ?: "") -> OptState.CORRECT
                opt == ui.chosen -> OptState.WRONG; else -> OptState.MUTED }
            OptionButton(opt, state, enabled = ui.chosen == null) { vm.choose(ui.options.indexOf(opt)) }
            Spacer(Modifier.height(8.dp)) }
        FeedbackText(if (ui.chosen == null) "" else "It was ${d?.item?.name}. ${d?.item?.explanation}",
            ui.chosen?.let { it == ui.answerIndex })
        Spacer(Modifier.height(24.dp)) }
}
@Composable
fun ChordTrainerScreen(onBack: () -> Unit) {
    val vm: ChordGameViewModel = viewModel(); val ui by vm.ui.collectAsState()
    LaunchedEffect(ui.data, ui.round) {
        if (ui.data != null && ui.chosen == null && !ui.finished) vm.play() }
    if (ui.finished) { val best by rememberBest("best_chord")
        GameScaffold("Chord Quality", onBack) {
            ResultPanel("You scored ${ui.score} of ${vm.totalRounds * 10}", "Best: $best") { vm.restart() } }
        return }
    GameScaffold("Chord Quality", onBack) {
        ScoreRow("Round ${ui.round} of ${vm.totalRounds}", "Score: ${ui.score}")
        InfoCard("What quality is this chord?")
        Row(Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Button(onClick = { vm.play() }, modifier = Modifier.weight(1f)) { Text("Play chord") }
            OutlinedButton(onClick = { if (ui.chosen == null) vm.play() },
                modifier = Modifier.weight(1f).padding(start = 8.dp)) { Text("Replay") } }
        Spacer(Modifier.height(10.dp))
        ui.options.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth()) { row.forEach { label ->
                val i = ui.options.indexOf(label)
                val state = when { ui.chosen == null -> OptState.IDLE
                    i == ui.answerIndex -> OptState.CORRECT
                    i == ui.chosen -> OptState.WRONG; else -> OptState.MUTED }
                OptionButton(label, state, enabled = ui.chosen == null,
                    modifier = Modifier.weight(1f).padding(2.dp)) { vm.choose(i) } } } }
        FeedbackText(if (ui.chosen == null) "" else "It was ${ui.options[ui.answerIndex]}",
            ui.chosen?.let { it == ui.answerIndex })
        Spacer(Modifier.height(24.dp)) }
}
