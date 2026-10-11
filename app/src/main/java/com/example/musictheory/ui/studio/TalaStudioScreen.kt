package com.example.musictheory.ui.studio
import android.content.Intent
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musictheory.audio.MidiWriter
import com.example.musictheory.audio.TonePlayer
import com.example.musictheory.data.ProgressRepo
import com.example.musictheory.ui.common.GameScaffold
import com.example.musictheory.ui.common.RhythmWheel
import com.example.musictheory.ui.common.ScoreRow
import com.example.musictheory.util.safeStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
private data class Preset(val name: String, val slots: Int, val slotMs: Int, val accents: List<Int>)
private val presets = listOf(Preset("Teentaal", 16, 300, listOf(0,4,8,12)),
    Preset("Keherwa", 8, 300, listOf(0,4)), Preset("Dadra", 6, 320, listOf(0,3)),
    Preset("Clave 3-2", 16, 160, listOf(0,3,6,10,12)),
    Preset("Bembé bell", 12, 170, listOf(0,2,3,5,7,8,10)), Preset("Aksak 7/8", 7, 230, listOf(0,2,4)),
    Preset("Soleá", 12, 190, listOf(2,5,7,9,10)), Preset("Kopanitsa", 11, 190, listOf(0,2,4,7,9)))
@Composable
fun TalaStudioScreen(onBack: () -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var slots by remember { mutableIntStateOf(16) }
    var slotMs by remember { mutableIntStateOf(300) }
    var grid by remember { mutableStateOf(List(16) { 1 }) }
    var playing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var currentSlot by remember { mutableIntStateOf(-1) }
    var tapMode by remember { mutableStateOf(false) }
    val tappedSlots = remember { mutableStateListOf<Int>() }
    var showSave by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }
    val currentGrid by rememberUpdatedState(grid)
    val saved by remember { ProgressRepo.patterns(context) }.collectAsState(emptyList())
    val cs = MaterialTheme.colorScheme
    DisposableEffect(Unit) { onDispose { TonePlayer.stopAll() } }
    LaunchedEffect(playing, slots, slotMs) { if (!playing) return@LaunchedEffect
        var next = SystemClock.elapsedRealtime(); var i = 0
        while (isActive) { currentSlot = i
            when (currentGrid[i]) { 2 -> TonePlayer.playTone(1320.0, 80, 0.9)
                1 -> TonePlayer.playTone(660.0, 55, 0.45); else -> TonePlayer.playTone(330.0, 40, 0.12) }
            next += slotMs; val wait = next - SystemClock.elapsedRealtime()
            delay(if (wait > 0) wait else 1L); i = (i + 1) % slots } }
    LaunchedEffect(playing, slots, slotMs) { if (!playing) return@LaunchedEffect
        val start = SystemClock.elapsedRealtime(); val cycle = (slots * slotMs).toFloat()
        while (isActive) { progress = ((SystemClock.elapsedRealtime() - start) % cycle.toLong()) / cycle
            androidx.compose.runtime.withFrameNanos { } } }
    GameScaffold("Tala Studio", onBack) {
        ScoreRow("$slots steps · $slotMs ms", "Tap a step: rest → beat → clap")
        Text("Presets", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp)) {
            presets.forEach { p -> FilterChip(false, { playing = false; slots = p.slots; slotMs = p.slotMs
                grid = List(p.slots) { i -> if (i in p.accents) 2 else 1 } },
                { Text(p.name) }, modifier = Modifier.padding(end = 6.dp)) } }
        Text("Steps per cycle", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp)) {
            listOf(6,7,8,11,12,15,16).forEach { n ->
                FilterChip(slots == n, { playing = false; slots = n; grid = List(n) { 1 } },
                    { Text("$n") }, modifier = Modifier.padding(end = 6.dp)) } }
        RhythmWheel(slots = slots, accents = grid.withIndex().filter { it.value == 2 }.map { it.index }.toSet(),
            progressProvider = { progress }, slotStates = grid,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Tempo", fontSize = 13.sp)
            Slider(slotMs.toFloat(), { slotMs = it.toInt() }, valueRange = 140f..400f,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)); Text("$slotMs ms") }
        Text("Cycle: %.1f s".format(slots * slotMs / 1000.0), fontSize = 12.sp, color = cs.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Tap mode", fontSize = 13.sp, modifier = Modifier.weight(1f))
            Switch(tapMode, { tapMode = it; if (!it) tappedSlots.clear() }) }
        if (tapMode) { Text("Tap each accent as the playhead passes it, then snap:", fontSize = 12.sp,
                color = cs.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Button(onClick = { if (currentSlot >= 0) tappedSlots.add(currentSlot) }, enabled = playing) {
                    Text("TAP") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { grid = grid.mapIndexed { i, st ->
                    if (i in tappedSlots.toSet()) 2 else st }; tappedSlots.clear() },
                    enabled = tappedSlots.isNotEmpty()) { Text("Snap (${tappedSlots.size})") } } }
        Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            grid.chunked(4).forEachIndexed { r, rowVals ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    rowVals.forEachIndexed { c, st -> val i = r * 4 + c
                        Box(Modifier.weight(1f).padding(2.dp).height(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(when (st) { 2 -> cs.primary; 1 -> cs.secondaryContainer
                                else -> cs.surfaceVariant })
                            .clickable { grid = grid.toMutableList().also { it[i] = (it[i] + 1) % 3 } },
                            contentAlignment = Alignment.Center) {
                            Text("${i + 1}", color = when (st) { 2 -> cs.onPrimary
                                1 -> cs.onSecondaryContainer; else -> cs.onSurfaceVariant },
                                fontWeight = FontWeight.Bold) } } } } }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Button(onClick = { playing = !playing }, modifier = Modifier.weight(1f)) {
                Icon(if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, null)
                Text(if (playing) " Stop" else " ▶ Play cycle") }
            OutlinedButton(onClick = { showSave = true }, enabled = !playing,
                modifier = Modifier.weight(1f).padding(start = 8.dp)) { Icon(Icons.Filled.Save, null); Text("  Save") } }
        OutlinedButton(onClick = { scope.launch(Dispatchers.IO) {
            val bpm = (60000.0 / (slotMs * 4)).toInt().coerceIn(20, 300)
            val notes = grid.mapIndexedNotNull { i, st -> when (st) {
                2 -> MidiWriter.Note(76, i * 120, 110, 110)
                1 -> MidiWriter.Note(64, i * 120, 110, 80); else -> null } }
            if (notes.isNotEmpty()) MidiWriter.exportBytes(context, notes, bpm)?.let { uri ->
                withContext(Dispatchers.Main) { context.safeStart(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply { type = "audio/midi"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Export MIDI"),
                    "Nothing can share MIDI files") } } }
        }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("🎸 Export MIDI") }
        Text("Saved patterns", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
        if (saved.isEmpty()) Text("None yet — craft a cycle and save it.", fontSize = 13.sp, color = cs.onSurfaceVariant)
        saved.forEach { p ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = cs.surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("${p.slots} steps · ${p.slotMs} ms", fontSize = 12.sp, color = cs.onSurfaceVariant) }
                    TextButton(onClick = { playing = false; slots = p.slots; slotMs = p.slotMs
                        grid = p.pattern.split(",").map { it.trim().toInt() } }) { Text("Load") }
                    IconButton(onClick = { scope.launch { ProgressRepo.deletePattern(context, p) } }) {
                        Icon(Icons.Filled.Delete, null) } } } }
        Spacer(Modifier.height(24.dp)) }
    if (showSave) AlertDialog(onDismissRequest = { showSave = false }, title = { Text("Save pattern") },
        text = { OutlinedTextField(saveName, { saveName = it }, label = { Text("Name") }, singleLine = true) },
        confirmButton = { TextButton(enabled = saveName.isNotBlank(), onClick = {
            scope.launch { ProgressRepo.savePattern(context, saveName.trim(), slots, slotMs, grid.joinToString(",")) }
            saveName = ""; showSave = false }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { showSave = false }) { Text("Cancel") } })
}
