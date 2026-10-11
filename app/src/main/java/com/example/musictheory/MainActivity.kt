package com.example.musictheory

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.musictheory.audio.TonePlayer
import com.example.musictheory.data.ContentRepo
import com.example.musictheory.data.ProgressRepo
import com.example.musictheory.data.SettingsRepo
import com.example.musictheory.data.ThemeMode
import com.example.musictheory.model.Category
import com.example.musictheory.ui.OnboardingOverlay
import com.example.musictheory.ui.games.*
import com.example.musictheory.ui.screens.*
import com.example.musictheory.ui.studio.TalaStudioScreen
import com.example.musictheory.ui.theme.HarmoniaTheme
import com.example.musictheory.ui.tools.*
import com.example.musictheory.ui.voice.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    @Volatile private var contentReady = false
    private val navEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !contentReady }
        if (BuildConfig.DEBUG) StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
        intent?.getStringExtra("route")?.let { navEvents.tryEmit(it) }
        lifecycleScope.launch {
            SettingsRepo.migrateLegacy(applicationContext)
            withContext(Dispatchers.IO) { ContentRepo.load { assets.open("content/$it") } }
            ProgressRepo.syncAllPaths(applicationContext)
            contentReady = true }
        TonePlayer.attach(this)
        setContent {
            val appContext = applicationContext
            val themeMode by remember { SettingsRepo.themeModeFlow(appContext) }
                .collectAsState(ThemeMode.SYSTEM)
            val dyn by remember { SettingsRepo.dynamicColorFlow(appContext) }.collectAsState(false)
            val wsc = calculateWindowSizeClass(this)
            HarmoniaTheme(darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }, dynamicColor = dyn) {
                App(compact = wsc.widthSizeClass == WindowWidthSizeClass.Compact,
                    navEvents = navEvents) } } }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent)
        intent.getStringExtra("route")?.let { navEvents.tryEmit(it) } }
}

private data class TabItem(val route: String, val icon: ImageVector, val labelRes: Int)

@Composable
fun App(compact: Boolean, navEvents: MutableSharedFlow<String>) {
    val nav = rememberNavController()
    val back by nav.currentBackStackEntryAsState()
    val route = back?.destination?.route ?: "home"
    val showBar = route == "home" || route == "games" || route.startsWith("topics/")
    val context = LocalContext.current
    val onboarded by remember { SettingsRepo.onboardedFlow(context) }.collectAsState(true)
    var askCrash by remember { mutableStateOf(false) }
    var crashTrace by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        when (SettingsRepo.crashOptInFlow(context).firstOrNull()) {
            null -> askCrash = true; true -> CrashReporterInstall(context) }
        CrashPending(context)?.let { crashTrace = it } }
    LaunchedEffect(Unit) {
        SettingsRepo.volumeFlow(context).collect { TonePlayer.masterVolume = it } }
    LaunchedEffect(Unit) { navEvents.collect { r ->
        val known = setOf("daily","review","tools","games","paths","stats","achievements",
            "search","glossary","keyboard","timeline","quizset","settings","creative","recordings")
        if (r in known) nav.navigate(r) { launchSingleTop = true } } }
    Scaffold(bottomBar = { if (showBar) BottomBar(nav, route) }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            NavHost(nav, startDestination = "home", modifier = Modifier.fillMaxSize()) {
                composable("home") { HomeScreen(onOpenCategory = { nav.navigate("topics/$it") },
                    onOpenGames = { nav.navigate("games") }, onOpen = { nav.navigate(it) }) }
                composable("topics/{cat}") { entry ->
                    val cat = Category.valueOf(entry.arguments?.getString("cat") ?: "WESTERN")
                    if (compact) TopicListScreen(cat) { i -> nav.navigate("topic/${cat.name}/$i") }
                    else TopicTwoPaneScreen(cat, onQuizMe = { k -> nav.navigate("topicquiz/$k") },
                        onRelated = { k -> val p = k.split("_")
                            nav.navigate("topic/${p[0]}/${p[1]}") }) }
                composable("topic/{cat}/{i}") { entry ->
                    val cat = Category.valueOf(entry.arguments?.getString("cat") ?: "WESTERN")
                    val i = entry.arguments?.getString("i")?.toIntOrNull() ?: 0
                    TopicDetailScreen(cat, i, onBack = { nav.popBackStack() },
                        onQuizMe = { k -> nav.navigate("topicquiz/$k") },
                        onRelated = { k -> val p = k.split("_")
                            nav.navigate("topic/${p[0]}/${p[1]}") }) }
                composable("topicquiz/{key}") { entry ->
                    TopicQuizScreen(entry.arguments?.getString("key") ?: "") { nav.popBackStack() } }
                composable("games") { GamesHubScreen { nav.navigate(it) } }
                composable("game/quiz") { QuizGameScreen { nav.popBackStack() } }
                composable("game/match") { MatchGameScreen { nav.popBackStack() } }
                composable("game/mode") { ModeGameScreen { nav.popBackStack() } }
                composable("game/interval") { IntervalGameScreen { nav.popBackStack() } }
                composable("game/note") { NoteHuntScreen { nav.popBackStack() } }
                composable("game/ragamaqam") { RagaMaqamScreen { nav.popBackStack() } }
                composable("game/chord") { ChordTrainerScreen { nav.popBackStack() } }
                composable("game/degree") { ScaleDegreeScreen { nav.popBackStack() } }
                composable("game/echo") { MelodyEchoScreen { nav.popBackStack() } }
                composable("game/dictation") { MelodyDictationScreen { nav.popBackStack() } }
                composable("game/lightning") { LightningScreen { nav.popBackStack() } }
                composable("game/passplay") { PassPlayScreen { nav.popBackStack() } }
                composable("game/staff") { StaffReaderScreen { nav.popBackStack() } }
                composable("game/scalebuilder") { ScaleBuilderScreen { nav.popBackStack() } }
                composable("game/chordbuilder") { ChordBuilderScreen { nav.popBackStack() } }
                composable("game/keysig") { KeySigDrillScreen { nav.popBackStack() } }
                composable("game/compare") { CompareModeScreen { nav.popBackStack() } }
                composable("game/prog") { ProgressionEarScreen { nav.popBackStack() } }
                composable("game/chordtone") { ChordTargetScreen { nav.popBackStack() } }
                composable("game/steady") { SteadinessScreen { nav.popBackStack() } }
                composable("game/rhythm") { RhythmTapScreen { nav.popBackStack() } }
                composable("game/rhythmread") { RhythmReadScreen { nav.popBackStack() } }
                composable("quizset") { QuizSetupScreen({ nav.popBackStack() },
                    { nav.navigate("quizset/$it") }, { nav.navigate("review") }) }
                composable("quizset/{id}") { entry ->
                    QuizSetGameScreen(entry.arguments?.getString("id") ?: "all") { nav.popBackStack() } }
                composable("studio/tala") { TalaStudioScreen { nav.popBackStack() } }
                composable("tools") { ToolsHubScreen({ nav.navigate(it) }) { nav.popBackStack() } }
                composable("tools/circle") { CircleOfFifthsScreen { nav.popBackStack() } }
                composable("tools/progressions") { ProgressionPlayerScreen { nav.popBackStack() } }
                composable("tools/metronome") { MetronomeScreen { nav.popBackStack() } }
                composable("tools/intervals") { IntervalChartScreen { nav.popBackStack() } }
                composable("tools/harmonic") { HarmonicExplorerScreen { nav.popBackStack() } }
                composable("tools/tuninglab") { TuningLabScreen { nav.popBackStack() } }
                composable("tools/drone") { DronePedalScreen { nav.popBackStack() } }
                composable("timeline") { TimelineScreen { nav.popBackStack() } }
                composable("search") { SearchScreen({ nav.popBackStack() }) { cat, i ->
                    nav.navigate("topic/${cat.name}/$i") } }
                composable("glossary") { GlossaryScreen { nav.popBackStack() } }
                composable("review") { ReviewScreen { nav.popBackStack() } }
                composable("keyboard") { WorldKeyboardScreen { nav.popBackStack() } }
                composable("recordings") { RecordingsScreen { nav.popBackStack() } }
                composable("daily") { DailyScreen { nav.popBackStack() } }
                composable("creative") { CreativeDailyScreen { nav.popBackStack() } }
                composable("paths") { PathsScreen({ nav.popBackStack() }) { nav.navigate("path/$it") } }
                composable("path/{id}") { entry ->
                    PathDetailScreen(entry.arguments?.getString("id") ?: "",
                        onBack = { nav.popBackStack() },
                        onOpenStep = { step -> when (step.kind) {
                            com.example.musictheory.model.PathStep.Kind.TOPIC -> {
                                val p = step.key.split("_")
                                nav.navigate("topic/${p[0]}/${p[1]}") }
                            com.example.musictheory.model.PathStep.Kind.GAME -> nav.navigate(step.key) } }) }
                composable("achievements") { AchievementsScreen { nav.popBackStack() } }
                composable("stats") { StatsScreen { nav.popBackStack() } }
                composable("settings") { SettingsScreen { nav.popBackStack() } }
                composable("voice/tuner") { TunerScreen { nav.popBackStack() } }
                composable("voice/drone") { MatchDroneScreen { nav.popBackStack() } }
                composable("voice/interval") { SingIntervalScreen { nav.popBackStack() } } }
            if (!onboarded) OnboardingOverlay(
                onPick = { id -> nav.navigate("path/$id"); SettingsRepo.setOnboarded(context, true) },
                onSkip = { SettingsRepo.setOnboarded(context, true) })
            if (askCrash) AlertDialog(onDismissRequest = { askCrash = false },
                title = { Text("Help improve Harmonia?") },
                text = { Text("If the app ever crashes, keep a local report you can choose to email. Nothing leaves your device without your tap.") },
                confirmButton = { TextButton(onClick = {
                    com.example.musictheory.util.CrashReporter.install(context)
                    SettingsRepo.setCrashOptIn(context, true); askCrash = false }) { Text("Allow") } },
                dismissButton = { TextButton(onClick = {
                    SettingsRepo.setCrashOptIn(context, false); askCrash = false }) { Text("No thanks") } })
            crashTrace?.let { trace ->
                AlertDialog(onDismissRequest = { crashTrace = null },
                    title = { Text("Report last crash?") },
                    text = { Text("A crash report is stored on this device. Send by email (review it first) or delete.") },
                    confirmButton = { TextButton(onClick = {
                        com.example.musictheory.util.CrashReporter.sendMail(context, trace)
                        com.example.musictheory.util.CrashReporter.clear(context); crashTrace = null }) { Text("Send") } },
                    dismissButton = { TextButton(onClick = {
                        com.example.musictheory.util.CrashReporter.clear(context); crashTrace = null }) { Text("Delete") } }) } } }
}

@Composable private fun CrashReporterInstall(c: android.content.Context) =
    com.example.musictheory.util.CrashReporter.install(c)
private fun CrashPending(c: android.content.Context) =
    kotlinx.coroutines.runBlocking { kotlinx.coroutines.withContext(Dispatchers.IO) {
        com.example.musictheory.util.CrashReporter.pending(c) } }

@Composable
private fun BottomBar(nav: NavHostController, current: String) {
    val tabs = listOf(TabItem("home", Icons.Filled.Home, R.string.tab_home),
        TabItem("topics/WESTERN", Icons.Filled.MusicNote, R.string.tab_western),
        TabItem("topics/EASTERN", Icons.Filled.SelfImprovement, R.string.tab_eastern),
        TabItem("topics/WORLD", Icons.Filled.Public, R.string.tab_world),
        TabItem("games", Icons.Filled.SportsEsports, R.string.tab_games))
    NavigationBar { tabs.forEach { tab ->
        NavigationBarItem(selected = current == tab.route, onClick = {
            nav.navigate(tab.route) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true; restoreState = true } },
            icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
            label = { Text(stringResource(tab.labelRes)) }) } }
}
