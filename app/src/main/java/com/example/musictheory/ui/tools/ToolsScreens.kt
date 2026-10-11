package com.example.musictheory.ui.tools
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musictheory.audio.Timbre
import com.example.musictheory.audio.TonePlayer
import com.example.musictheory.ui.common.GameScaffold
import com.example.musictheory.ui.common.ToggleRow
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.roundToInt
import kotlin.math.sin
@Composable
fun ToolsHubScreen(onOpen: (String) -> Unit, onBack: () -> Unit) {
    val tools = listOf("tools/circle" to ("⭕  Circle of Fifths" to "Tap a key — hear it, read its signature."),
        "tools/progressions" to ("🎶  Progression Player" to "I–V–vi–IV, ii–V–I, blues, Andalusian…"),
        "tools/metronome" to ("⏱  Metronome" to "40–240 BPM, tap tempo, subdivisions."),
        "tools/intervals" to ("📏  Interval Chart" to "Every interval, playable, any root."),
        "tools/harmonic" to ("🔊  Harmonic Explorer" to "Tap partials 1–12 — khoomei's engine."),
        "tools/tuninglab" to ("🧪  Tuning Lab" to "Hear just vs Pythagorean vs 12-TET beats."),
        "tools/drone" to ("🎻  Drone Pedal" to "Sustained Sa–Pa in any key — practice partner."),
        "recordings" to ("📼  My Takes" to "Play, share or delete your recordings."),
        "timeline" to ("📜  History Timeline" to "Pythagoras to bossa — 2,500 years."))
    GameScaffold("Practice Tools", onBack) {
        tools.forEach { (route, label) ->
            Card(onClick = { onOpen(route) }, shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(label.first, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(label.second, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
}
private val majors = listOf("C","G","D","A","E","B","F♯","D♭","A♭","E♭","B♭","F")
private val minors = listOf("Am","Em","Bm","F♯m","C♯m","G♯m","E♭m","B♭m","Fm","Cm","Gm","Dm")
private val sharpsOrder = listOf("F♯","C♯","G♯","D♯","A♯","E♯","B♯")
private val flatsOrder = listOf("B♭","E♭","A♭","D♭","G♭","C♭","F♭")
private val noteNames = arrayOf("C","C♯","D","D♯","E","F","F♯","G","G♯","A","A♯","B")
private fun keyRootSemi(name: String) = when (name[0]) { 'C' -> 0; 'D' -> 2; 'E' -> 4; 'F' -> 5
    'G' -> 7; 'A' -> 9; else -> 11 } + (if (name.contains("♯")) 1 else if (name.contains("♭")) -1 else 0)
@Composable
fun CircleOfFifthsScreen(onBack: () -> Unit) {
    var selected by remember { mutableIntStateOf(0) }
    val measurer = rememberTextMeasurer(); val cs = MaterialTheme.colorScheme
    val key = majors[selected]
    val sig = when { selected in 1..6 -> "${selected}♯: " + sharpsOrder.take(selected).joinToString(" ")
        selected >= 7 -> "${12 - selected}♭: " + flatsOrder.take(12 - selected).joinToString(" ")
        else -> "No accidentals" }
    val majorLayouts = remember(selected) { majors.map {
        measurer.measure(it, TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)) } }
    val minorLayouts = remember(selected) { minors.map { measurer.measure(it, TextStyle(fontSize = 13.sp)) } }
    GameScaffold("Circle of Fifths", onBack) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f).padding(top = 8.dp)
            .pointerInput(Unit) { detectTapGestures { off ->
                val dx = off.x - size.width / 2f; val dy = off.y - size.height / 2f
                var ang = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
                ang = (ang + 90 + 360) % 360
                selected = (((ang + 15) / 30).toInt() % 12 + 12) % 12 } }) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val rOut = size.minDimension / 2f * 0.96f; val rIn = rOut * 0.55f; val band = rOut - rIn
            for (i in 0 until 12) {
                drawArc(if (i == selected) cs.primaryContainer else cs.surface, i * 30f - 105f, 30f, false,
                    style = Stroke(band, cap = StrokeCap.Butt),
                    topLeft = Offset(c.x - (rIn + rOut) / 2f, c.y - (rIn + rOut) / 2f),
                    size = Size(rIn + rOut, rIn + rOut))
                val mid = Math.toRadians((i * 30 - 90).toDouble())
                fun put(l: androidx.compose.ui.text.TextLayoutResult, radius: Float) {
                    drawText(l, topLeft = Offset(
                        c.x + (cos(mid) * radius).toFloat() - l.size.width / 2f,
                        c.y + (sin(mid) * radius).toFloat() - l.size.height / 2f)) }
                put(majorLayouts[i], rOut - band * 0.3f); put(minorLayouts[i], rIn + band * 0.25f) }
            drawCircle(cs.outline.copy(alpha = 0.4f), rOut, c, style = Stroke(2f)) }
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("$key major · relative ${minors[selected]}", fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, color = cs.primary)
                Text(sig, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.padding(top = 12.dp)) {
                    Button(onClick = { TonePlayer.playScale(listOf(0.0,2.0,4.0,5.0,7.0,9.0,11.0,12.0),
                        rootSemi = keyRootSemi(key).toDouble(), timbre = Timbre.PLUCK) }) { Text("▶ Major scale") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { TonePlayer.playScale(listOf(0.0,2.0,3.0,5.0,7.0,8.0,10.0,12.0),
                        rootSemi = (keyRootSemi(key) + 9).toDouble(), timbre = Timbre.PLUCK) }) {
                        Text("▶ Relative minor") } } } } }
}
private data class PStep(val root: Int, val minor: Boolean)
private data class Prog(val name: String, val steps: List<PStep>)
private val progs = listOf(Prog("Pop — I V vi IV",
    listOf(PStep(0,false), PStep(7,false), PStep(9,true), PStep(5,false))),
    Prog("Jazz — ii V I", listOf(PStep(2,true), PStep(7,false), PStep(0,false))),
    Prog("50s — I vi IV V", listOf(PStep(0,false), PStep(9,true), PStep(5,false), PStep(7,false))),
    Prog("Andalusian", listOf(PStep(9,true), PStep(7,false), PStep(5,false), PStep(4,false))),
    Prog("12-bar blues", listOf(PStep(0,false), PStep(0,false), PStep(0,false), PStep(0,false),
        PStep(5,false), PStep(5,false), PStep(0,false), PStep(0,false),
        PStep(7,false), PStep(5,false), PStep(0,false), PStep(7,false))))
@Composable
fun ProgressionPlayerScreen(onBack: () -> Unit) {
    var keyRoot by remember { mutableIntStateOf(0) }
    var progIdx by remember { mutableIntStateOf(0) }
    var arp by remember { mutableStateOf(false) }
    GameScaffold("Progression Player", onBack) {
        Text("Key", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp)) {
            noteNames.forEachIndexed { i, nm ->
                FilterChip(keyRoot == i, { keyRoot = i }, { Text(nm) }, Modifier.padding(end = 4.dp)) } }
        Text("Progression", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp)) {
            progs.forEachIndexed { i, p ->
                FilterChip(progIdx == i, { progIdx = i }, { Text(p.name.substringBefore(" —")) },
                    Modifier.padding(end = 4.dp)) } }
        val p = progs[progIdx]
        Text(p.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        Text("in ${noteNames[keyRoot]}:  " + p.steps.joinToString(" – ") {
            noteNames[(it.root + keyRoot) % 12] + if (it.minor) "m" else "" },
            fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Arpeggiate", fontSize = 13.sp); Switch(arp, { arp = it }, Modifier.padding(start = 4.dp))
            Spacer(Modifier.weight(1f))
            Button(onClick = { val chords = p.steps.map { st -> val r = st.root + keyRoot
                (if (st.minor) listOf(0,3,7) else listOf(0,4,7)).map { TonePlayer.Notes.freqD(r + it) } }
                if (arp) TonePlayer.playArpeggiatedProgression(chords)
                else TonePlayer.playChordSequence(chords, 700, 120, Timbre.PLUCK) }) { Text("▶ Play") } }
        Text("Chord tones stack from each degree — hear how the loop wants to go home.",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp)) }
}
@Composable
fun MetronomeScreen(onBack: () -> Unit) {
    var bpm by remember { mutableIntStateOf(100) }
    var beats by remember { mutableIntStateOf(4) }
    var subdiv by remember { mutableIntStateOf(1) }
    var playing by remember { mutableStateOf(false) }
    var current by remember { mutableIntStateOf(-1) }
    val tapTimes = remember { mutableListOf<Long>() }
    val cs = MaterialTheme.colorScheme
    LaunchedEffect(playing, beats, bpm, subdiv) {
        if (!playing) { current = -1; return@LaunchedEffect }
        val slot = 60000L / bpm
        var next = android.os.SystemClock.elapsedRealtime(); var i = 0
        while (isActive) { current = i
            if (i % subdiv == 0) TonePlayer.playTone(1320.0, 80, 0.9, Timbre.BELL)
            else TonePlayer.playTone(880.0, 50, 0.5)
            next += slot / subdiv
            delay((next - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(1L)); i++ } }
    GameScaffold("Metronome", onBack) {
        Text("$bpm BPM", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        Slider(bpm.toFloat(), { bpm = it.toInt() }, valueRange = 40f..240f,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Button(onClick = { val now = android.os.SystemClock.elapsedRealtime(); tapTimes.add(now)
            if (tapTimes.size > 4) tapTimes.removeAt(0)
            if (tapTimes.size >= 2) { val iois = tapTimes.zipWithNext { a, b -> b - a }
                bpm = (60000.0 / iois.average()).roundToInt().coerceIn(40, 240) } },
            modifier = Modifier.fillMaxWidth()) { Text("👋 Tap tempo") }
        Text("Beats per bar", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
        Row(Modifier.padding(top = 6.dp)) { listOf(1,2,3,4).forEach { n ->
            FilterChip(beats == n, { beats = n; playing = false }, { Text("$n") }, Modifier.padding(end = 6.dp)) } }
        Text("Subdivisions", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
        Row(Modifier.padding(top = 6.dp)) { listOf(1,2,3,4).forEach { n ->
            FilterChip(subdiv == n, { subdiv = n }, { Text("$n") }, Modifier.padding(end = 6.dp)) } }
        Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.Center) {
            repeat(beats) { i -> Box(Modifier.padding(6.dp).size(34.dp)
                .background(if (i == current && playing) cs.primary else cs.surfaceVariant, CircleShape)) } }
        Button(onClick = { playing = !playing }, modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
            Text(if (playing) "⏹ Stop" else "▶ Start", fontWeight = FontWeight.Bold) } }
}
private val intervalNames = listOf("Unison","Minor 2nd","Major 2nd","Minor 3rd","Major 3rd",
    "Perfect 4th","Tritone","Perfect 5th","Minor 6th","Major 6th","Minor 7th","Major 7th","Octave")
@Composable
fun IntervalChartScreen(onBack: () -> Unit) {
    var root by remember { mutableIntStateOf(0) }
    var harmonic by remember { mutableStateOf(false) }
    GameScaffold("Interval Chart", onBack) {
        Text("Root", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp)) {
            noteNames.forEachIndexed { i, nm ->
                FilterChip(root == i, { root = i }, { Text(nm) }, Modifier.padding(end = 4.dp)) } }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Harmonic (together)", fontSize = 13.sp)
            Switch(harmonic, { harmonic = it }, Modifier.padding(start = 4.dp))
            Text("· else melodic", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.height(10.dp))
        intervalNames.forEachIndexed { n, nm ->
            Card(onClick = { if (harmonic) TonePlayer.playChord(listOf(
                TonePlayer.Notes.freqD(root.toDouble()), TonePlayer.Notes.freqD(root + n)), 1100)
                else TonePlayer.playFrequencies(listOf(TonePlayer.Notes.freqD(root.toDouble()),
                    TonePlayer.Notes.freqD(root + n))) }, shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(nm, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("$n semitones · ${TonePlayer.Notes.name(root)}→${TonePlayer.Notes.name(root + n)}",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        Spacer(Modifier.height(24.dp)) }
}
@Composable
fun HarmonicExplorerScreen(onBack: () -> Unit) {
    var root by remember { mutableIntStateOf(0) }
    var stackUpTo by remember { mutableIntStateOf(1) }
    fun partialHz(n: Int) = TonePlayer.Notes.freqD(root) * n
    fun centsOff(n: Int) = 12.0 * log2(n.toDouble()) - (12.0 * log2(n.toDouble())).roundToInt()
    val names = listOf("Unison","Octave","Octave + fifth","2nd octave","2nd octave + major 3rd",
        "2nd octave + fifth","≈ flat minor 7th (31¢ low!)","3rd octave","3rd octave + whole tone",
        "≈ just below minor 3rd","≈ tritone +49¢","3rd octave + fifth")
    GameScaffold("Harmonic Series Explorer", onBack) {
        Text("Every note is a chord of overtones. Khoomei singers isolate these one by one.",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp)) {
            noteNames.forEachIndexed { i, nm ->
                FilterChip(root == i, { root = i; TonePlayer.stopAll() }, { Text(nm) },
                    modifier = Modifier.padding(end = 4.dp)) } }
        Text("Partial", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
        (1..12).forEach { n ->
            Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(
                containerColor = if (n <= stackUpTo) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("$n×", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp))
                    Column(Modifier.weight(1f)) {
                        Text(names[n - 1], fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("%.2f Hz · %+.0f¢ from 12-TET".format(partialHz(n), centsOff(n)),
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    TextButton(onClick = { TonePlayer.playTone(partialHz(n), 900, 0.7, Timbre.SINE) }) {
                        Text("▶") } } } }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Button(onClick = { TonePlayer.stopAll()
                TonePlayer.playChord((1..stackUpTo).map { partialHz(it) }, 2200, Timbre.BELL) },
                modifier = Modifier.weight(1f)) { Text("Stack 1–$stackUpTo") }
            OutlinedButton(onClick = { stackUpTo = stackUpTo % 12 + 1 },
                modifier = Modifier.weight(1f).padding(start = 8.dp)) { Text("Add partial →") } }
        OutlinedButton(onClick = { TonePlayer.stopAll()
            TonePlayer.playFrequencies((1..12).map { partialHz(it) }, noteMs = 260, gapMs = 60, timbre = Timbre.BELL) },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("▶ Sweep the whole series") }
        Text("Notice partial 7 (31¢ flat) and 11 (+49¢) — the piano's compromise starts here.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.height(24.dp)) }
}
@Composable
fun TuningLabScreen(onBack: () -> Unit) {
    var root by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var temperament by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) { onDispose { TonePlayer.stopAll() } }
    val thirdSemis = listOf(3.8631, 4.0782, 4.0)
    val thirdRatios = listOf("5:4 (just)", "81:64 (Pythagorean)", "2^(4/12) (equal)")
    val beatHz = abs(TonePlayer.Notes.freqD(root) * 5.0 -
        TonePlayer.Notes.freqD(root + thirdSemis[temperament]) * 4.0)
    LaunchedEffect(playing, root, temperament) { if (!playing) return@LaunchedEffect
        while (isActive) { TonePlayer.playChord(listOf(TonePlayer.Notes.freqD(root - 12.0),
            TonePlayer.Notes.freqD(root - 12.0 + 7.0196),
            TonePlayer.Notes.freqD(root - 12.0 + thirdSemis[temperament])), 1600, Timbre.SINE)
            delay(1800) } }
    GameScaffold("Tuning Lab", onBack) {
        Text("A sustained major triad. Switch the third while it sounds — hear the beats vanish.",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.padding(top = 12.dp)) { listOf("Just","Pythagorean","12-TET").forEachIndexed { i, nm ->
            FilterChip(temperament == i, { temperament = i }, { Text(nm) },
                modifier = Modifier.padding(end = 6.dp)) } }
        Text("Third = ${thirdRatios[temperament]} · %.2f semitones · beats ≈ %.1f Hz"
            .format(thirdSemis[temperament], if (temperament == 0) 0.0 else beatHz),
            fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp)) {
            listOf("C" to 0,"D" to 2,"E" to 4,"F" to 5,"G" to 7,"A" to 9).forEach { (nm, s) ->
                FilterChip(root == s, { root = s; TonePlayer.stopAll() }, { Text(nm) },
                    Modifier.padding(end = 4.dp)) } }
        Button(onClick = { if (playing) TonePlayer.stopAll(); playing = !playing },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text(if (playing) "⏹ Stop" else "▶ Sustain the triad", fontWeight = FontWeight.Bold) }
        Text("Just: perfectly still. Pythagorean: the 3rd beats violently. 12-TET: a gentle shimmer — the compromise you've heard your whole life.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.height(24.dp)) }
}
@Composable
fun DronePedalScreen(onBack: () -> Unit) {
    var root by remember { mutableIntStateOf(0) }
    var fifth by remember { mutableStateOf(true) }
    var octave by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { TonePlayer.stopAll() } }
    LaunchedEffect(playing, root, fifth, octave) { if (!playing) return@LaunchedEffect
        val freqs = buildList { add(TonePlayer.Notes.freqD(root - 12.0))
            if (fifth) add(TonePlayer.Notes.freqD(root - 5.0))
            if (octave) add(TonePlayer.Notes.freqD(root.toDouble())) }
        while (isActive) { freqs.forEach { TonePlayer.playTone(it, 4200, 0.22) }; delay(4000) } }
    GameScaffold("Drone Pedal", onBack) {
        Text("A sustained tanpura-style Sa–Pa in any key — practice against it, sing against it.",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Tonic", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp)) {
            noteNames.forEachIndexed { i, nm ->
                FilterChip(root == i, { root = i
                    if (playing) { playing = false; TonePlayer.stopAll() } }, { Text(nm) },
                    modifier = Modifier.padding(end = 4.dp)) } }
        ToggleRow("Pa (fifth)", fifth) { fifth = it }
        ToggleRow("Upper octave", octave) { octave = it }
        Button(onClick = { if (playing) TonePlayer.stopAll(); playing = !playing },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text(if (playing) "⏹ Stop drone" else "▶ Start drone", fontWeight = FontWeight.Bold) }
        Text("Now in ${noteNames[root]}", fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.height(24.dp)) }
}
