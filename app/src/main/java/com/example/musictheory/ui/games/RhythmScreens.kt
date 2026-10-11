package com.example.musictheory.ui.games
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musictheory.data.RhythmPatterns
import com.example.musictheory.ui.common.*
import com.example.musictheory.vm.RhythmGameViewModel
import com.example.musictheory.vm.RhythmReadViewModel
import com.example.musictheory.vm.RhythmTapViewModel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withFrameNanos
@Composable
fun RhythmGameScreen(title: String, vm: RhythmGameViewModel, earMode: Boolean, onBack: () -> Unit) {
    val ui by vm.ui.collectAsState()
    DisposableEffect(Unit) { onDispose { vm.stop() } }
    if (ui.finished) { val best by rememberBest(if (earMode) "best_rhythm" else "best_rhythmread")
        GameScaffold(title, onBack) {
            ResultPanel("Total: ${ui.totalScore} points", "Best: $best") { vm.restart() } }
        return }
    val p = RhythmPatterns.all.getOrNull(ui.patternIdx)
    var progress by remember { mutableStateOf(0f) }
    if (p != null) LaunchedEffect(ui.patternIdx, ui.phase) { if (ui.phase <= 1) {
        val start = SystemClock.elapsedRealtime(); val cyc = (p.slots * p.slotMs).toFloat()
        while (isActive && vm.ui.value.phase <= 1) {
            progress = ((SystemClock.elapsedRealtime() - start) % cyc.toLong()) / cyc
            withFrameNanos { } } } }
    GameScaffold(title, onBack) {
        ScoreRow("Round ${ui.round} of 5", "Score: ${ui.totalScore}")
        if (p != null) { InfoCard(p.name)
            Text(p.note, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)) }
        if (p != null) RhythmWheel(slots = p.slots, accents = p.accents.toSet(),
            progressProvider = { progress },
            tapTolerance = ui.tolMs / (p.slots * p.slotMs.toFloat()),
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
        when (ui.phase) {
            0 -> Text(if (earMode) "🎧 Listen…" else "📖 Memorise the highlighted steps…",
                fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp))
            1 -> { Text("Tap the accented (high) beats! · ±${ui.tolMs}ms", fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
                Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Button(onClick = { vm.tap() }, shape = CircleShape, modifier = Modifier.size(180.dp)) {
                        Text("TAP", fontSize = 32.sp, fontWeight = FontWeight.Bold) } } }
            2 -> Column(Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("You caught ${ui.hits} of ${ui.maxHits} accents", fontSize = 18.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                Button(onClick = { vm.next() }) { Text(if (ui.round < 5) "Next pattern" else "See results") } } }
        Spacer(Modifier.height(24.dp)) }
}
@Composable fun RhythmTapScreen(onBack: () -> Unit) {
    val vm: RhythmTapViewModel = viewModel()
    RhythmGameScreen("Rhythm Tap", vm, earMode = true, onBack) }
@Composable fun RhythmReadScreen(onBack: () -> Unit) {
    val vm: RhythmReadViewModel = viewModel()
    RhythmGameScreen("Rhythm Reading", vm, earMode = false, onBack) }
