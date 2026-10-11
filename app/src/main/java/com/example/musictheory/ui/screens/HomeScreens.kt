package com.example.musictheory.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musictheory.R
import com.example.musictheory.audio.TonePlayer
import com.example.musictheory.data.*
import com.example.musictheory.model.Category
import com.example.musictheory.model.TheoryTopic
import kotlinx.coroutines.launch
@Composable
fun HomeScreen(onOpenCategory: (String) -> Unit, onOpenGames: () -> Unit, onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Harmonia", fontSize = 34.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.home_tagline), fontSize = 15.sp, lineHeight = 21.sp,
            modifier = Modifier.padding(top = 6.dp))
        Text(stringResource(R.string.home_sub), fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
        DailyCard(onOpen); FactCard(); StatsCard(); ReviewButton(onOpen); QuickChips(onOpen); PathBanner(onOpen)
        Spacer(Modifier.height(14.dp))
        CategoryCard(stringResource(R.string.cat_western), stringResource(R.string.sub_western),
            Color(0xFF4F46E5)) { onOpenCategory("WESTERN") }
        CategoryCard(stringResource(R.string.cat_eastern), stringResource(R.string.sub_eastern),
            Color(0xFF0D9488)) { onOpenCategory("EASTERN") }
        CategoryCard(stringResource(R.string.cat_world), stringResource(R.string.sub_world),
            Color(0xFFD97706)) { onOpenCategory("WORLD") }
        CategoryCard(stringResource(R.string.cat_games), stringResource(R.string.sub_games),
            Color(0xFFBE123C)) { onOpenGames() }
        Spacer(Modifier.height(28.dp)) }
}
@Composable private fun DailyCard(onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val done by remember { SettingsRepo.dailyDoneFlow(context) }.collectAsState(false)
    val streak by remember { SettingsRepo.streakFlow(context) }.collectAsState(0)
    val cDone by remember { SettingsRepo.creativeDoneFlow(context) }.collectAsState(false)
    Card(onClick = { onOpen("daily") }, shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (done) "✅" else "🔥", fontSize = 26.sp)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(stringResource(R.string.home_daily), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(if (done) stringResource(R.string.home_daily_done)
                        else stringResource(R.string.home_streak, streak), fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            TextButton(onClick = { onOpen("creative") }, modifier = Modifier.padding(top = 2.dp)) {
                Text(if (cDone) "🎵 Creative challenge — done ✓" else "🎵 Today's creative challenge →",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold) } } }
}
@Composable private fun FactCard() {
    val idx = Math.floorMod((System.currentTimeMillis() / 86_400_000L).toInt(), Facts.all.size)
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.fact_label), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary)
            Text(Facts.all[idx], fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 4.dp)) } }
}
@Composable private fun StatsCard() {
    val context = LocalContext.current
    val stats by remember { ProgressRepo.homeStats(context) }.collectAsState(HomeStats())
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Row(Modifier.padding(16.dp)) {
            Column(Modifier.weight(1f)) { Text("📚", fontSize = 22.sp)
                Text("${stats.topicsStudied}/41", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Text("topics studied", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Column(Modifier.weight(1f)) { Text("🎮", fontSize = 22.sp)
                Text("${stats.gamesPlayed}", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Text("games played", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
}
@Composable private fun ReviewButton(onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val due by remember { ProgressRepo.dueCount(context) }.collectAsState(0)
    Button(onClick = { onOpen("review") }, shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(if (due > 0) stringResource(R.string.home_review_due, due)
            else stringResource(R.string.home_review_ok), fontWeight = FontWeight.Bold) }
}
@Composable private fun QuickChips(onOpen: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 10.dp)) {
        listOf("search" to R.string.chip_search, "glossary" to R.string.chip_glossary,
            "paths" to R.string.chip_paths, "stats" to R.string.chip_stats,
            "achievements" to R.string.chip_ach, "tools" to R.string.chip_tools,
            "keyboard" to R.string.chip_keyboard, "settings" to R.string.chip_settings)
            .forEach { (route, res) ->
                FilterChip(false, { onOpen(route) }, { Text(stringResource(res)) },
                    modifier = Modifier.padding(end = 6.dp)) } }
}
@Composable private fun PathBanner(onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val progress by remember { ProgressRepo.allPathProgress(context) }.collectAsState(emptyList())
    val suggested by remember { SettingsRepo.suggestedPath(context) }.collectAsState("")
    val inProgress = PathsData.all.firstOrNull { p ->
        val pp = progress.firstOrNull { it.pathId == p.id }
        pp != null && pp.completedSteps > 0 && pp.completedSteps < p.steps.size }
    val suggestedStart = PathsData.all.firstOrNull {
        it.id == suggested && progress.none { pp -> pp.pathId == it.id } }
    inProgress?.let { p -> Card(onClick = { onOpen("path/${p.id}") }, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4F46E5)),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text("${p.emoji}  ${stringResource(R.string.home_continue, p.title)}", color = Color.White,
            fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(16.dp)) } }
        ?: suggestedStart?.let { p -> Card(onClick = { onOpen("path/${p.id}") },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D9488)),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text("${p.emoji}  ${stringResource(R.string.home_start, p.title)}", color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(16.dp)) } }
}
@Composable private fun CategoryCard(title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(20.dp))
        .background(color).clickable(onClick = onClick).padding(18.dp)) {
        Column { Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp)) } }
}
private fun titleFor(cat: Category) = when (cat) {
    Category.WESTERN -> "Western Classical Theory"
    Category.EASTERN -> "Eastern Musical Traditions"
    Category.WORLD -> "World Music Theory" }
@Composable
fun TopicListScreen(category: Category, onOpen: (Int) -> Unit) {
    val context = LocalContext.current
    val studied by remember { ProgressRepo.studiedKeys(context) }.collectAsState(emptySet())
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Text(titleFor(category), fontSize = 24.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp))
        TopicListContent(category, studied, onOpen) }
}
@Composable
fun TopicListContent(category: Category, studied: Set<String>, onOpen: (Int) -> Unit) {
    val topics = TheoryData.topicsFor(category)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
        itemsIndexed(topics, key = { i, _ -> "${category.name}_$i" }) { i, t ->
            val key = "${category.name}_$i"
            Card(onClick = { onOpen(i) }, shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("%02d".format(i + 1), fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(t.tagline, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (key in studied) Text("✓", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary) } } } }
}
@Composable
fun TopicDetailScreen(category: Category, index: Int, onBack: () -> Unit,
                      onQuizMe: (String) -> Unit, onRelated: (String) -> Unit) {
    val t = TheoryData.topicsFor(category)[index]
    val context = LocalContext.current
    val key = "${category.name}_$index"
    var slow by remember { mutableStateOf(false) }
    var loop by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    val droneRoot by remember { SettingsRepo.droneRootFlow(context) }.collectAsState(0)
    val bookmarked by remember { SettingsRepo.bookmarkFlow(context, key) }.collectAsState(false)
    val demos = remember(key) { ScaleDemos.forTopic(category, index) }
    val links = remember(key) { ListenLinks.forTopic(key) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(key) { ProgressRepo.markRead(context, key, t.title) }
    DisposableEffect(key) { onDispose { TonePlayer.stopAll() } }
    fun playDemo(d: com.example.musictheory.model.ScaleDemo) {
        TonePlayer.stopAll()
        val cue = ListenCues.forDemo(key, d.name)
        if (d.asChord) TonePlayer.playChord(
            d.semitones.map { TonePlayer.Notes.freqD(it + droneRoot) }, 1100, d.timbre)
        else if (cue != null) scope.launch {
            for (i in 3 downTo 1) { countdown = i; kotlinx.coroutines.delay(650) }
            countdown = 0
            if (loop) TonePlayer.playScaleLoop(d.semitones, noteMs = if (slow) 620 else 380,
                gapMs = if (slow) 150 else 90, withDrone = d.withDrone,
                rootSemi = droneRoot.toDouble(), timbre = d.timbre)
            else TonePlayer.playScale(d.semitones, noteMs = if (slow) 620 else 380,
                gapMs = if (slow) 150 else 90, withDrone = d.withDrone,
                rootSemi = droneRoot.toDouble(), timbre = d.timbre) }
        else if (loop) TonePlayer.playScaleLoop(d.semitones, noteMs = if (slow) 620 else 380,
            gapMs = if (slow) 150 else 90, withDrone = d.withDrone,
            rootSemi = droneRoot.toDouble(), timbre = d.timbre)
        else TonePlayer.playScale(d.semitones, noteMs = if (slow) 620 else 380,
            gapMs = if (slow) 150 else 90, withDrone = d.withDrone,
            rootSemi = droneRoot.toDouble(), timbre = d.timbre)
        ProgressRepo.recordDemo(context, key, t.title) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { SettingsRepo.toggleBookmark(context, key) }) {
                Icon(if (bookmarked) Icons.Filled.Star else Icons.Filled.StarBorder, "Bookmark",
                    tint = if (bookmarked) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant) } }
        Text(t.title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(t.tagline, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
        Text(t.body, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 14.dp))
        Button(onClick = { onQuizMe(key) }, shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
            Text("🧩  Quiz me on this topic", fontWeight = FontWeight.Bold) }
        if (demos.isNotEmpty()) {
            Text("🎧 Listen", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 24.dp))
            Text("Drone / tonic root:", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp)) {
                listOf("C","C♯","D","D♯","E","F","F♯","G","G♯","A","A♯","B").forEachIndexed { i, nm ->
                    FilterChip(droneRoot == i, { TonePlayer.stopAll(); SettingsRepo.setDroneRoot(context, i) },
                        { Text(nm) }, modifier = Modifier.padding(end = 4.dp)) } }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Slow", fontSize = 13.sp); Switch(slow, { slow = it }, Modifier.padding(start = 4.dp))
                Text("Loop", fontSize = 13.sp, modifier = Modifier.padding(start = 14.dp))
                Switch(loop, { loop = it }, Modifier.padding(start = 4.dp))
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = { TonePlayer.stopAll() }) { Text("⏹ Stop") } }
            demos.forEach { d ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(d.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            ListenCues.forDemo(key, d.name)?.let { cue ->
                                Text("👂 Listen for: $cue", fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(top = 2.dp)) }
                            if (d.note.isNotEmpty()) Text(d.note, fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)) }
                        Button(onClick = { playDemo(d) }) {
                            Text(if (countdown > 0) "$countdown" else "▶ Play") } } } } }
        if (links.isNotEmpty()) {
            Text("📻 Further listening", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 24.dp))
            links.forEach { item ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f))
                        IconButton(onClick = { ListenLinks.open(context, item) }) {
                            Icon(Icons.Filled.OpenInNew, "Open") } } } } }
        if (t.related.isNotEmpty()) {
            Text("🔗 Related topics", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 18.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp)) {
                t.related.forEach { r ->
                    AssistChip(onClick = { onRelated(r) }, label = {
                        Text(ContentRepo.topicById(r)?.title ?: r) },
                        modifier = Modifier.padding(end = 6.dp)) } } }
        Spacer(Modifier.height(28.dp)) }
}
@Composable
fun TopicTwoPaneScreen(category: Category, onQuizMe: (String) -> Unit, onRelated: (String) -> Unit) {
    val context = LocalContext.current
    val studied by remember { ProgressRepo.studiedKeys(context) }.collectAsState(emptySet())
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.weight(0.38f)) {
            Column(Modifier.fillMaxSize().background(cs.background)) {
                Text(titleFor(category), fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                TopicListContent(category, studied) { selected = it } } }
        Box(Modifier.width(1.dp).fillMaxHeight().background(cs.outline.copy(alpha = 0.4f)))
        Box(Modifier.weight(0.62f)) {
            if (selected >= 0) TopicDetailScreen(category, selected, {}, onQuizMe, onRelated)
            else Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Text("👈 Pick a topic", fontSize = 15.sp, color = cs.onSurfaceVariant) } } }
}
