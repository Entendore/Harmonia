package com.example.musictheory.ui.screens
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.musictheory.audio.MidiWriter
import com.example.musictheory.audio.Timbre
import com.example.musictheory.audio.TonePlayer
import com.example.musictheory.audio.WavRecorder
import com.example.musictheory.data.SettingsRepo
import com.example.musictheory.data.ThemeMode
import com.example.musictheory.ui.common.*
import com.example.musictheory.util.safeStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    val volume by remember { SettingsRepo.volumeFlow(context) }.collectAsState(1f)
    val haptics by remember { SettingsRepo.hapticsFlow(context) }.collectAsState(true)
    val themeMode by remember { SettingsRepo.themeModeFlow(context) }.collectAsState(ThemeMode.SYSTEM)
    val dynamic by remember { SettingsRepo.dynamicColorFlow(context) }.collectAsState(false)
    var confirmReset by remember { mutableStateOf(false) }
    GameScaffold("Settings", onBack) {
        Text("Appearance", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
        Row(Modifier.padding(top = 6.dp)) {
            listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")
                .forEach { (m, label) ->
                    FilterChip(themeMode == m, { scope.launch { SettingsRepo.setThemeMode(context, m) } },
                        { Text(label) }, modifier = Modifier.padding(end = 6.dp)) } }
        ToggleRow("Material You dynamic color", dynamic) {
            scope.launch { SettingsRepo.setDynamicColor(context, it) } }
        Text("Sound", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
        Text("Volume: ${(volume * 100).toInt()}%", fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(volume, { scope.launch { SettingsRepo.setVolume(context, it) } }, valueRange = 0.2f..1f,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        ToggleRow("Haptic feedback", haptics) { scope.launch { SettingsRepo.setHaptics(context, it) } }
        OutlinedButton(onClick = { TonePlayer.stopAll() },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("⏹ Stop all audio") }
        Text("Data", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp))
        Text("Everything lives on this device only.", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { confirmReset = true },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Reset all progress", color = MaterialTheme.colorScheme.error) }
        Text("Harmonia 5.1 · works offline · no account, no tracking", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 20.dp)) }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false },
        title = { Text("Reset everything?") },
        text = { Text("Topics, scores, streaks, review cards, saved patterns and settings will be erased. Cannot be undone.") },
        confirmButton = { TextButton(onClick = {
            scope.launch { SettingsRepo.resetAll(context) }; confirmReset = false }) { Text("Reset") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } })
}
@Composable
fun RecordingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingPath by remember { mutableStateOf<String?>(null) }
    val fmt = remember { SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()) }
    fun stop() { player?.release(); player = null; playingPath = null }
    fun refresh() { scope.launch { files = withContext(Dispatchers.IO) {
        File(context.cacheDir, "recordings").listFiles()?.filter { it.name.endsWith(".wav") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList() } } }
    LaunchedEffect(Unit) { refresh() }
    DisposableEffect(Unit) { onDispose { stop() } }
    GameScaffold("📼 My takes", onBack) {
        if (files.isEmpty()) Text("No recordings yet — make one in the World Keyboard.",
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp))
        files.forEach { f ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(fmt.format(Date(f.lastModified())), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("${f.length() / 1024} KB", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    TextButton(onClick = {
                        if (playingPath == f.path) stop()
                        else scope.launch {
                            val p = withContext(Dispatchers.IO) { MediaPlayer.create(context, Uri.fromFile(f)) }
                            if (p != null) { stop(); player = p; playingPath = f.path
                                p.setOnCompletionListener { stop() }; p.start() } } }) {
                        Text(if (playingPath == f.path) "⏹" else "▶") }
                    IconButton(onClick = {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", f)
                        context.safeStart(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "audio/wav"; putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share take"),
                            "Nothing can share right now") }) { Icon(Icons.Filled.Share, null) }
                    IconButton(onClick = { if (playingPath == f.path) stop(); f.delete(); refresh() }) {
                        Icon(Icons.Filled.Delete, null) } } } }
        Spacer(Modifier.height(24.dp)) }
}
private val kbScales = listOf("Major pent" to listOf(0.0,2.0,4.0,7.0,9.0),
    "Major" to listOf(0.0,2.0,4.0,5.0,7.0,9.0,11.0), "Nat minor" to listOf(0.0,2.0,3.0,5.0,7.0,8.0,10.0),
    "Harm minor" to listOf(0.0,2.0,3.0,5.0,7.0,8.0,11.0), "Blues" to listOf(0.0,3.0,5.0,6.0,7.0,10.0),
    "Hirajoshi" to listOf(0.0,2.0,3.0,7.0,8.0), "Yo" to listOf(0.0,2.0,5.0,7.0,9.0),
    "Rast" to listOf(0.0,2.0,3.5,5.0,7.0,9.0,10.5), "Bayati" to listOf(0.0,1.5,3.0,5.0,7.0,8.0,10.0),
    "Hijaz" to listOf(0.0,1.0,4.0,5.0,7.0,8.0,10.0), "Slendro" to listOf(0.0,2.4,4.8,7.2,9.6),
    "Pelog" to listOf(0.0,1.2,3.4,5.6,7.6), "Freygish" to listOf(0.0,1.0,4.0,5.0,7.0,8.0,10.0))
@Composable
fun WorldKeyboardScreen(onBack: () -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var scaleIdx by rememberSaveable { mutableIntStateOf(0) }
    var octave by rememberSaveable { mutableIntStateOf(0) }
    var labelMode by rememberSaveable { mutableIntStateOf(0) }
    var timbre by remember { mutableStateOf(Timbre.PLUCK) }
    var recording by remember { mutableStateOf(false) }
    var pendingTake by remember { mutableStateOf<File?>(null) }
    var secs by remember { mutableIntStateOf(0) }
    val scale = kbScales[scaleIdx].second
    val semis = remember(scale) { (scale + scale.map { it + 12 }).distinct().sorted().take(12) }
    val freqs = remember(semis, octave) { semis.map { TonePlayer.Notes.freqD(it + octave) } }
    val labels = remember(semis, labelMode) { when (labelMode) {
        0 -> semis.mapIndexed { i, _ -> "${i + 1}" }
        1 -> semis.map { solfege(it, sargam = false) }
        else -> semis.map { solfege(it, sargam = true) } } }
    LaunchedEffect(recording) { while (recording && isActive) {
        secs = WavRecorder.recordedSeconds()
        if (WavRecorder.isFull) { recording = false
            pendingTake = withContext(Dispatchers.IO) { WavRecorder.stop(context) } }
        delay(500) } }
    GameScaffold("World Keyboard", onBack) {
        Text(kbScales[scaleIdx].first, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp))
        Text("Scale", fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp)) {
            kbScales.forEachIndexed { i, (nm, _) ->
                FilterChip(scaleIdx == i, { scaleIdx = i }, { Text(nm) },
                    modifier = Modifier.padding(end = 4.dp)) } }
        Row(Modifier.padding(top = 10.dp)) {
            listOf(-12 to "-1", 0 to "0", 12 to "+1").forEach { (v, nm) ->
                FilterChip(octave == v, { octave = v }, { Text(nm) }, modifier = Modifier.padding(end = 6.dp)) } }
        Row(Modifier.padding(top = 4.dp)) {
            listOf("1·2·3", "Do·Re·Mi", "Sa·Re·Ga").forEachIndexed { i, nm ->
                FilterChip(labelMode == i, { labelMode = i }, { Text(nm) },
                    modifier = Modifier.padding(end = 6.dp)) } }
        Row(Modifier.padding(top = 4.dp)) {
            listOf("Pluck" to Timbre.PLUCK, "Sine" to Timbre.SINE, "Bell" to Timbre.BELL)
                .forEach { (nm, t) ->
                    FilterChip(timbre == t, { timbre = t }, { Text(nm) },
                        modifier = Modifier.padding(end = 6.dp)) } }
        WorldKeyboard(freqs, labels, onKey = { _, f -> TonePlayer.playTone(f, 800, 0.85, timbre) },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
        if (scale.any { it % 1.0 != 0.0 }) Text(
            "⚠ This scale contains true quarter tones — no piano can play it, you can.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(top = 8.dp))
        when {
            !recording && pendingTake == null ->
                Button(onClick = { WavRecorder.start(); secs = 0; recording = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Text("⏺ Record a take", fontWeight = FontWeight.Bold) }
            recording -> {
                Button(onClick = { recording = false; scope.launch {
                    pendingTake = withContext(Dispatchers.IO) { WavRecorder.stop(context) } } },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Text("⏹ Finish take", fontWeight = FontWeight.Bold) }
                Text("⏺ ${secs / 60}:${"%02d".format(secs % 60)} · cap 3:00", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(top = 6.dp)) }
            else -> Row(Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("✅ Take saved", fontSize = 14.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { val f = pendingTake ?: return@TextButton
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", f)
                    context.safeStart(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "audio/wav"; putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share take"),
                        "Nothing can share right now") }) { Text("Share") }
                TextButton(onClick = { pendingTake?.delete(); pendingTake = null }) { Text("Discard") } } }
        OutlinedButton(onClick = { scope.launch(Dispatchers.IO) {
            val notes = semis.mapIndexed { i, s -> MidiWriter.Note((60 + s.toInt() + octave), i * 240, 220) }
            MidiWriter.exportBytes(context, notes, 120)?.let { uri -> withContext(Dispatchers.Main) {
                context.safeStart(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "audio/midi"; putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Export scale as MIDI"),
                    "Nothing can share MIDI files") } } }
        }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("🎸 Export scale as MIDI") }
        Text("Takes live in Tools → 📼 My takes.", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(24.dp)) }
}
private val creativeScales = listOf("Hirajoshi" to listOf(0.0,2.0,3.0,7.0,8.0),
    "Rast" to listOf(0.0,2.0,3.5,5.0,7.0,9.0,10.5), "Major pent" to listOf(0.0,2.0,4.0,7.0,9.0),
    "Blues" to listOf(0.0,3.0,5.0,6.0,7.0,10.0), "Freygish" to listOf(0.0,1.0,4.0,5.0,7.0,8.0,10.0))
private val prompts = listOf("Start on the 5th and end on the tonic.",
    "Use one rest — silence is a note.", "Leap once, then walk back by steps.",
    "Repeat a 3-note idea, higher each time.", "Make the first note the longest.",
    "Answer your own phrase — call and response.",
    "Climb all the way up, then fall to the root.", "Two short, one long — breathe.")
@Composable
fun CreativeDailyScreen(onBack: () -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    val done by remember { SettingsRepo.creativeDoneFlow(context) }.collectAsState(false)
    val streak by remember { SettingsRepo.creativeStreakFlow(context) }.collectAsState(0)
    val seed = System.currentTimeMillis() / 86_400_000L
    val rnd = remember(seed) { Random(seed) }
    val (scaleName, scale) = creativeScales[rnd.nextInt(creativeScales.size)]
    val root = rnd.nextInt(12); val prompt = prompts[rnd.nextInt(prompts.size)]
    val semis = remember(scaleName) { (scale + scale.map { it + 12 }).distinct().sorted().take(12) }
    val freqs = remember(semis, root) { semis.map { TonePlayer.Notes.freqD(it + root) } }
    val NAMES = arrayOf("C","C♯","D","D♯","E","F","F♯","G","G♯","A","A♯","B")
    GameScaffold("🎵 Creative Daily", onBack) {
        Text("$scaleName in ${NAMES[root]}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Prompt: $prompt", fontSize = 15.sp, color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 4.dp))
        Text("🔥 $streak-day creative streak", fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp))
        if (done) Text("✅ Saved today — hear it in My takes.", fontSize = 14.sp,
            color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 12.dp))
        WorldKeyboard(freqs, semis.mapIndexed { i, _ -> "${i + 1}" },
            onKey = { _, f -> TonePlayer.playTone(f, 700, 0.85, Timbre.PLUCK) },
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
        if (!done) Row(Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Button(onClick = { WavRecorder.start() }, modifier = Modifier.weight(1f)) {
                Text("⏺ Take the challenge") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { scope.launch(Dispatchers.IO) {
                WavRecorder.stop(context); SettingsRepo.markCreativeDone(context) } },
                enabled = WavRecorder.isRecording, modifier = Modifier.weight(1f)) { Text("⏹ Save") } }
        Text("Same scale + prompt for everyone, all day. Improvise a short phrase and save the take.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.height(24.dp)) }
}
@Composable
fun TimelineScreen(onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val events = listOf("c. 500 BCE" to ("Pythagoras & the ratios" to "Harmony tied to number — 2:1 octave, 3:2 fifth."),
        "13th c." to ("Saṅgīta Ratnākara" to "Sarngadeva codifies ragas, shrutis and dance."),
        "c. 1025" to ("Guido of Arezzo" to "Solfège and the staff — music becomes writable."),
        "1691" to ("Werckmeister's temperament" to "The case for well-tempered keyboards."),
        "1722" to ("Well-Tempered Clavier" to "Bach proves 24 keys in sound."),
        "1889" to ("Debussy meets gamelan" to "The Paris Expo rewires Western harmony."),
        "1903" to ("Handy hears the blues" to "A Mississippi station platform."),
        "1958" to ("'Chega de Saudade'" to "Bossa nova is born."),
        "1959" to ("Kind of Blue" to "Modal jazz — the Dorian dawn."),
        "1972" to ("Mulatu's 'Yèkatit'" to "Ethio-jazz fuses qenet with funk."))
    GameScaffold("📜 History Timeline", onBack) {
        Text("2,500 years of music theory, East and West.", fontSize = 13.sp, color = cs.onSurfaceVariant)
        events.forEach { (year, t) ->
            Card(shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text(year, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cs.secondary)
                    Text(t.first, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(t.second, fontSize = 13.sp, lineHeight = 18.sp, color = cs.onSurfaceVariant) } } }
        Spacer(Modifier.height(24.dp)) }
}
