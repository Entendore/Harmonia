package com.example.musictheory.ui.screens
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musictheory.data.*
import com.example.musictheory.data.db.LeitnerCard
import com.example.musictheory.model.Category
import com.example.musictheory.model.Question
import com.example.musictheory.reach.ShareCard
import com.example.musictheory.ui.common.*
import com.example.musictheory.util.findActivity
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
@Composable
fun GamesHubScreen(onPlay: (String) -> Unit) {
    val context = LocalContext.current
    val bests by remember { SettingsRepo.bests(context) }.collectAsState(emptyMap())
    data class E(val emoji: String, val title: String, val desc: String, val route: String, val key: String)
    val sections = listOf(
        "🧠 Knowledge" to listOf(
            E("🧠","Theory Quiz","10 random questions across all traditions.","game/quiz","best_quiz"),
            E("🌍","Match the Tradition","Which culture does each concept belong to?","game/match","best_match"),
            E("🎼","Name the Mode","Turn seven mode colours into reflexes.","game/mode","best_mode"),
            E("📄","Staff Reader","Treble & bass clef, ledger lines, four tiers.","game/staff","best_staff"),
            E("👥","Pass & Play","Two players, alternating questions.","game/passplay","best_passplay"),
            E("🧩","Custom Practice","Western, Eastern, World or Rhythm pool.","quizset",""),
            E("🛠","Scale Builder","Construct scales from their step pattern.","game/scalebuilder","best_scalebuild"),
            E("🧱","Chord Builder","Stack the thirds yourself.","game/chordbuilder","best_chordbuild"),
            E("🔑","Key Signature Drill","60-second sprint around the Circle of Fifths.","game/keysig","best_keysig")),
        "👂 Ear Training" to listOf(
            E("👂","Interval Ear Training","Two tones play — name the distance.","game/interval","best_interval"),
            E("🎹","Note Hunt","A mystery note — find its key.","game/note","best_note"),
            E("🎭","Chord Quality","Hear major/minor/dim/aug/7th — name it.","game/chord","best_chord"),
            E("🪜","Scale Degrees","Tonic plus a note — which degree?","game/degree","best_degree"),
            E("🎶","Raga & Maqam Trainer","Identify the phrase — real quarter tones.","game/ragamaqam","best_ragamaqam"),
            E("📣","Melody Echo","Echo the phrase on the World Keyboard.","game/echo","best_echo"),
            E("🔁","Progression Ear","I–V–vi–IV or I–vi–IV–V? Name the loop.","game/prog","best_prog"),
            E("🕊","Chord Tone Target","Which tone sits over the drone?","game/chordtone","best_chordtone"),
            E("🆚","Compare Mode","Same tune, two modes — which was which?","game/compare","best_compare")),
        "🥁 Rhythm" to listOf(
            E("🥁","Rhythm Tap","Echo tala, clave, compás and usul by ear.","game/rhythm","best_rhythm"),
            E("👁","Rhythm Reading","See the accents — tap them from sight.","game/rhythmread","best_rhythmread"),
            E("🫀","Steadiness","Tap at a target BPM — score your drift.","game/steady","best_steady"),
            E("🪘","Tala Studio","Build your own cycle — quantize, save, export MIDI.","studio/tala","")),
        "🎤 Voice" to listOf(
            E("🎚","Tuner","Live needle — any tonic, any tradition.","voice/tuner",""),
            E("🧘","Match the Drone","Sing the Sa. Hold it. Be scored.","voice/drone","best_drone"),
            E("🎯","Sing the Interval","Hear it, sing it back — graded in cents.","voice/interval","best_sing")),
        "⚡ Play" to listOf(
            E("⚡","Lightning Round","60 seconds. Combo up to ×5.","game/lightning","best_lightning"),
            E("🎹","World Keyboard","Play any scale — record, export, share.","keyboard","")),
        "✍️ More" to listOf(
            E("✍️","Melody Dictation","Hear a phrase — place it on the grid.","game/dictation","best_dictation")))
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item(key = "hubtitle") { Text("Game Room", fontSize = 26.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 14.dp)) }
        sections.forEach { (title, games) ->
            item(key = "h:$title") { Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)) }
            games.forEach { g -> item(key = "g:${g.route}${g.title}") {
                Card(onClick = { onPlay(g.route) }, shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${g.emoji}  ${g.title}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(g.desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (g.key.isNotEmpty()) Text("Best: ${bests[g.key] ?: 0}", fontSize = 12.sp,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary) } } } } }
        item(key = "hubbottom") { Spacer(Modifier.height(24.dp)) } }
}
@Composable
fun ReviewScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var set by remember { mutableStateOf<List<Pair<Question, String>>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var idx by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf<Int?>(null) }
    var correctCount by remember { mutableIntStateOf(0) }
    var recorded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { set = ProgressRepo.buildReviewSet(context, 10); loaded = true }
    LaunchedEffect(chosen) { if (chosen != null) {
        kotlinx.coroutines.delay(1200); idx++; chosen = null } }
    if (!loaded) { GameScaffold("Spaced Review", onBack) { Text("Loading your cards…") }; return }
    if (set.isEmpty()) { GameScaffold("Spaced Review", onBack) {
        ResultPanel("Nothing due 🎉", "All caught up. Play games to earn new cards.") {} }; return }
    if (idx >= set.size) { if (!recorded) { recorded = true
        ProgressRepo.recordGame(context, "Spaced Review", correctCount * 10) }
        GameScaffold("Spaced Review", onBack) { ResultPanel("Reviewed ${set.size} cards",
            "$correctCount correct — shaky cards return sooner.") {} }; return }
    val (q, ref) = set[idx]
    GameScaffold("Spaced Review", onBack) {
        ScoreRow("Card ${idx + 1} of ${set.size}", "✓ $correctCount")
        InfoCard(q.text); Spacer(Modifier.height(14.dp))
        q.options.forEachIndexed { i, opt ->
            val state = when { chosen == null -> OptState.IDLE
                i == q.answer -> OptState.CORRECT; i == chosen -> OptState.WRONG; else -> OptState.MUTED }
            OptionButton(opt, state, enabled = chosen == null) { if (chosen == null) {
                chosen = i; val ok = i == q.answer; if (ok) correctCount++
                ProgressRepo.grade(context, ref, ok) } }
            Spacer(Modifier.height(8.dp)) }
        if (chosen != null) Text(q.explanation, fontSize = 14.sp, lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(24.dp)) }
}
@Composable
fun DailyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val done by remember { SettingsRepo.dailyDoneFlow(context) }.collectAsState(false)
    val streak by remember { SettingsRepo.streakFlow(context) }.collectAsState(0)
    var started by rememberSaveable { mutableStateOf(false) }
    if (started) { val seed = System.currentTimeMillis() / 86_400_000L
        val vm = rememberMcqViewModel("daily", "best_daily", "Daily Challenge",
            onFinishExtra = { SettingsRepo.markDailyDone(context) }) {
            QuestionRepo.all.indices.shuffled(kotlin.random.Random(seed)).take(10)
                .map { QuestionRepo.all[it] } }
        com.example.musictheory.ui.games.McqGameScaffold("Daily Challenge", vm, onBack); return }
    GameScaffold("Daily Challenge", onBack) {
        Column(Modifier.fillMaxWidth().padding(vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔥", fontSize = 44.sp)
            Text("$streak-day streak", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(if (done) "✅ Today's challenge complete — come back tomorrow!"
                else "10 seeded questions — everyone gets the same set today.",
                fontSize = 14.sp, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
            if (!done) Button(onClick = { started = true }, shape = RoundedCornerShape(14.dp),
                modifier = Modifier.padding(top = 20.dp)) {
                Text("Play today's challenge", fontWeight = FontWeight.Bold) } } }
}
@Composable
fun PathsScreen(onBack: () -> Unit, onOpenPath: (String) -> Unit) {
    val context = LocalContext.current
    val snap by remember { PathEngine.snapshotFlow(context) }
        .collectAsState(PathEngine.Snapshot(emptySet(), emptySet()))
    val suggested by remember { SettingsRepo.suggestedPath(context) }.collectAsState("")
    GameScaffold("Learning Paths", onBack) {
        Text("Curated journeys — read, listen, play.", fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        PathsData.all.forEach { p ->
            val done = PathEngine.completedCount(p, snap)
            Card(onClick = { onOpenPath(p.id) }, shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.emoji, fontSize = 24.sp)
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(p.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(p.blurb, fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Text("$done/${p.steps.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary) }
                    LinearProgressIndicator(progress = { done.toFloat() / p.steps.size },
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                    if (p.id == suggested && done < p.steps.size) Text("⭐ Suggested for you",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 6.dp)) } } } }
}
@Composable
fun PathDetailScreen(pathId: String, onBack: () -> Unit,
                     onOpenStep: (com.example.musictheory.model.PathStep) -> Unit) {
    val path = PathsData.byId[pathId] ?: return
    val context = LocalContext.current
    val snap by remember { PathEngine.snapshotFlow(context) }
        .collectAsState(PathEngine.Snapshot(emptySet(), emptySet()))
    val done = PathEngine.completedCount(path, snap)
    LaunchedEffect(done) { ProgressRepo.syncPath(context, pathId, done) }
    GameScaffold(path.title, onBack) {
        Text(path.blurb, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(progress = { done.toFloat() / path.steps.size },
                modifier = Modifier.weight(1f))
            Text("  $done / ${path.steps.size}", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        if (done < path.steps.size) Button(onClick = { onOpenStep(path.steps[done]) },
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Text(if (done == 0) "Start path →" else "Continue →", fontWeight = FontWeight.Bold) }
        else Text("🎉 Path complete!", fontSize = 16.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 14.dp))
        path.steps.forEachIndexed { i, step ->
            Card(onClick = { if (i <= done) onOpenStep(step) }, shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (i < done) "✅" else if (i <= done) "▶️" else "🔒", fontSize = 16.sp)
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(step.label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(if (step.kind == com.example.musictheory.model.PathStep.Kind.TOPIC)
                            "Read & listen" else "Play", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
        Spacer(Modifier.height(24.dp)) }
}
@Composable
fun AchievementsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val snap by remember {
        combine(combine(ProgressRepo.studiedKeys(context), ProgressRepo.playedGames(context),
            ProgressRepo.gamesCount(context)) { s, p, c -> Triple(s, p, c) },
            SettingsRepo.bests(context), SettingsRepo.streakFlow(context),
            ProgressRepo.pathsCompleted(context)) { t, b, st, pd ->
            AchSnap(t.first, t.second, t.third, b, st, pd) }.collectAsState(AchSnap()) }
    val earned = remember(snap) { Achievements.all.filter { it.test(snap) }.toSet() }
    val scope = rememberCoroutineScope()
    GameScaffold("Achievements — ${earned.size} / ${Achievements.all.size}", onBack) {
        Button(onClick = { context.findActivity()?.let {
            scope.launch { ShareCard.buildAndShare(it) } } },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) { Text("📤 Share progress card") }
        Achievements.all.forEach { a ->
            val got = a in earned
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(
                containerColor = if (got) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (got) a.emoji else "🔒", fontSize = 22.sp)
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(a.title, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            color = if (got) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(a.desc, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
        Spacer(Modifier.height(24.dp)) }
}
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val results by remember { ProgressRepo.allResults(context) }.collectAsState(emptyList())
    val bests by remember { SettingsRepo.bests(context) }.collectAsState(emptyMap())
    val counts = remember(results) { val today = System.currentTimeMillis() / 86_400_000L
        IntArray(14).also { arr -> results.forEach { r ->
            val age = (today - r.playedAt / 86_400_000L).toInt()
            if (age in 0..13) arr[13 - age]++ } } }
    val avg = remember(results) { if (results.isEmpty()) 0 else results.sumOf { it.score } / results.size }
    val cs = MaterialTheme.colorScheme
    GameScaffold("Your Stats", onBack) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(Modifier.weight(1f)) { Text("🎮", fontSize = 22.sp)
                Text("${results.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("total games", fontSize = 12.sp, color = cs.onSurfaceVariant) }
            Column(Modifier.weight(1f)) { Text("📈", fontSize = 22.sp)
                Text("$avg", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("avg score", fontSize = 12.sp, color = cs.onSurfaceVariant) } }
        Text("Games — last 14 days", fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 18.dp))
        Canvas(Modifier.fillMaxWidth().height(140.dp).padding(top = 8.dp)) {
            val max = counts.max().coerceAtLeast(1); val bw = size.width / 14f
            counts.forEachIndexed { i, c -> val h = if (c == 0) 4f else size.height * c / max
                drawRoundRect(cs.primary.copy(alpha = if (c == 0) 0.15f else 0.85f),
                    Offset(i * bw + bw * 0.15f, size.height - h), Size(bw * 0.7f, h),
                    cornerRadius = CornerRadius(6f, 6f)) } }
        Text("Best scores", fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 18.dp))
        bests.filterValues { it > 0 }.toList().sortedByDescending { it.second }.forEach { (k, v) ->
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(k.removePrefix("best_").replaceFirstChar { it.uppercase() }, fontSize = 14.sp,
                    modifier = Modifier.weight(1f))
                Text("$v", fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
        Spacer(Modifier.height(24.dp)) }
}
@Composable
fun QuizSetupScreen(onBack: () -> Unit, onStart: (String) -> Unit, onMisses: () -> Unit) {
    GameScaffold("Custom Practice", onBack) {
        Text("Pick the pool, then run a 10-question round.", fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        QuestionSets.options.forEach { (id, label, count) ->
            Card(onClick = { onStart(id) }, shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Text("$label  ·  $count questions", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)) } }
        Card(onClick = onMisses, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
            Text("🎯 My misses  ·  live in Spaced Review", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)) }
        Spacer(Modifier.height(20.dp)) }
}
@Composable
fun SearchScreen(onBack: () -> Unit, onOpenTopic: (Category, Int) -> Unit) {
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val bookmarks by remember { SettingsRepo.bookmarksSet(context) }.collectAsState(emptySet())
    data class Hit(val cat: Category, val index: Int, val title: String, val tagline: String, val key: String)
    val all = remember { Category.entries.flatMap { cat -> TheoryData.topicsFor(cat).mapIndexed { i, t ->
        Hit(cat, i, t.title, t.tagline, "${cat.name}_$i") } } }
    GameScaffold("Search & Bookmarks", onBack, scrollable = false) {
        OutlinedTextField(query, { query = it }, label = { Text("Search topics…") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
            if (query.isBlank() && bookmarks.isNotEmpty()) {
                item(key = "bmh") { Text("★ Bookmarks", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 6.dp)) }
                items(all.filter { it.key in bookmarks }, key = { "bm:${it.key}" }) { h ->
                    TopicRow(h.title, h.tagline, h.key in bookmarks,
                        { SettingsRepo.toggleBookmark(context, h.key) },
                        { onOpenTopic(h.cat, h.index) }) } }
            val results = if (query.isBlank()) all else all.filter {
                it.title.contains(query, true) || it.tagline.contains(query, true) }
            items(results, key = { "r:${it.key}" }) { h ->
                TopicRow(h.title, h.tagline, h.key in bookmarks,
                    { SettingsRepo.toggleBookmark(context, h.key) },
                    { onOpenTopic(h.cat, h.index) }) } } }
}
@Composable
private fun TopicRow(title: String, tagline: String, marked: Boolean, onStar: () -> Unit, onOpen: () -> Unit) {
    Card(onClick = onOpen, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(tagline, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = onStar) { Text(if (marked) "★" else "☆", fontSize = 20.sp) } } }
}
@Composable
fun GlossaryScreen(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { Glossary.terms.filter {
        it.term.contains(query, true) || it.definition.contains(query, true) } }
    GameScaffold("Glossary — ${results.size} terms", onBack, scrollable = false) {
        OutlinedTextField(query, { query = it },
            label = { Text("Search the world's music words…") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
            items(results, key = { it.term }) { t ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.term, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                            Text(t.definition, fontSize = 13.sp, lineHeight = 18.sp) }
                        GlossaryAudio.map[t.term]?.let { clip -> IconButton(onClick = {
                            com.example.musictheory.audio.TonePlayer.stopAll()
                            if (clip.chord) com.example.musictheory.audio.TonePlayer.playChord(
                                clip.semis.map { com.example.musictheory.audio.TonePlayer.Notes.freqD(it) })
                            else com.example.musictheory.audio.TonePlayer.playScale(
                                clip.semis, withDrone = clip.drone) }) { Text("▶") } } } } } } }
}
