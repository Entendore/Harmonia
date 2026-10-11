package com.example.musictheory.data
import android.content.Context; import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore; import com.example.musictheory.data.db.ProgressDb
import kotlinx.coroutines.Dispatchers; import kotlinx.coroutines.flow.*; import kotlinx.coroutines.withContext
enum class ThemeMode { SYSTEM, LIGHT, DARK }
val Context.harmoniaSettings by preferencesDataStore("harmonia_settings")
object SettingsRepo {
    private const val DAY = 86_400_000L
    private val Context.ds get() = harmoniaSettings
    private val K_DRONE = intPreferencesKey("drone_root"); private val K_BM = stringSetPreferencesKey("bookmarks")
    private val K_STREAK = intPreferencesKey("streak"); private val K_LAST = longPreferencesKey("last_daily_epoch")
    private val K_ONB = booleanPreferencesKey("onboarded"); private val K_SUG = stringPreferencesKey("suggested_path")
    private val K_MIG = booleanPreferencesKey("legacy_migrated"); private val K_LEIT = booleanPreferencesKey("leitner_ids_migrated")
    private val K_REV = longPreferencesKey("review_at"); private val K_CR = booleanPreferencesKey("crash_optin")
    private val K_CRK = booleanPreferencesKey("crash_known"); private val K_CLAST = longPreferencesKey("last_creative_epoch")
    private val K_CS = intPreferencesKey("creative_streak"); private val K_VOL = floatPreferencesKey("volume")
    private val K_HAP = booleanPreferencesKey("haptics"); private val K_TH = stringPreferencesKey("theme_mode")
    private val K_DYN = booleanPreferencesKey("dynamic_color")
    private fun K_BEST(k: String) = intPreferencesKey("best_$k")
    suspend fun migrateLegacy(context: Context) = withContext(Dispatchers.IO) {
        context.ds.edit { p -> if (p[K_MIG] != true) {
            context.getSharedPreferences("harmonia_prefs", Context.MODE_PRIVATE).all.forEach { (k, v) ->
                when { k == "drone_root" && v is Int -> p[K_DRONE] = v
                    k == "bookmarks" && v is Set<*> -> p[K_BM] = v.filterIsInstance<String>().toSet()
                    k.startsWith("best_") && v is Int -> { val key = intPreferencesKey(k); if (v > (p[key] ?: 0)) p[key] = v } } }
            p[K_MIG] = true } } }
    fun bestFlow(c: Context, k: String): Flow<Int> = c.ds.data.map { it[K_BEST(k)] ?: 0 }
    fun bests(c: Context): Flow<Map<String, Int>> = c.ds.data.map { p -> p.asMap().entries
        .filter { it.key.name.startsWith("best_") }.associate { it.key.name.removePrefix("best_") to (it.value as Int) } }
    suspend fun saveBest(c: Context, k: String, s: Int) { c.ds.edit { p -> if (s > (p[K_BEST(k)] ?: 0)) p[K_BEST(k)] = s } }
    fun droneRootFlow(c: Context): Flow<Int> = c.ds.data.map { it[K_DRONE] ?: 0 }
    suspend fun setDroneRoot(c: Context, v: Int) { c.ds.edit { it[K_DRONE] = v } }
    fun bookmarkFlow(c: Context, k: String): Flow<Boolean> = c.ds.data.map { k in (it[K_BM] ?: emptySet()) }
    fun bookmarksSet(c: Context): Flow<Set<String>> = c.ds.data.map { it[K_BM] ?: emptySet() }
    suspend fun toggleBookmark(c: Context, k: String) { c.ds.edit { p -> val cur = p[K_BM] ?: emptySet()
        p[K_BM] = if (k in cur) cur - k else cur + k } }
    fun streakFlow(c: Context): Flow<Int> = c.ds.data.map { it[K_STREAK] ?: 0 }
    fun dailyDoneFlow(c: Context): Flow<Boolean> = c.ds.data.map { (it[K_LAST] ?: 0L) == System.currentTimeMillis() / DAY }
    suspend fun markDailyDone(c: Context) { c.ds.edit { p -> val t = System.currentTimeMillis() / DAY
        val l = p[K_LAST] ?: 0L; if (l == t) return@edit
        p[K_STREAK] = if (l == t - 1) (p[K_STREAK] ?: 0) + 1 else 1; p[K_LAST] = t }
        com.example.musictheory.widget.refreshWidget(c) }
    fun creativeDoneFlow(c: Context): Flow<Boolean> = c.ds.data.map { (it[K_CLAST] ?: 0L) == System.currentTimeMillis() / DAY }
    fun creativeStreakFlow(c: Context): Flow<Int> = c.ds.data.map { it[K_CS] ?: 0 }
    suspend fun markCreativeDone(c: Context) { c.ds.edit { p -> val t = System.currentTimeMillis() / DAY
        val l = p[K_CLAST] ?: 0L; if (l == t) return@edit
        p[K_CS] = if (l == t - 1) (p[K_CS] ?: 0) + 1 else 1; p[K_CLAST] = t } }
    fun onboardedFlow(c: Context): Flow<Boolean> = c.ds.data.map { it[K_ONB] ?: false }
    suspend fun setOnboarded(c: Context, v: Boolean) { c.ds.edit { it[K_ONB] = v } }
    fun suggestedPath(c: Context): Flow<String> = c.ds.data.map { it[K_SUG] ?: "" }
    suspend fun setSuggestedPath(c: Context, v: String) { c.ds.edit { it[K_SUG] = v } }
    fun leitnerIdsMigrated(c: Context): Boolean = runBlocking1 { c.ds.data.map { it[K_LEIT] ?: false }.first() }
    @Suppress("FunctionName") private fun <T> runBlocking1(b: suspend () -> T): T =
        kotlinx.coroutines.runBlocking { b() }
    suspend fun isLeitnerIdsMigrated(c: Context): Boolean = c.ds.data.map { it[K_LEIT] ?: false }.first()
    suspend fun setLeitnerIdsMigrated(c: Context) { c.ds.edit { it[K_LEIT] = true } }
    fun crashOptInFlow(c: Context): Flow<Boolean?> = c.ds.data.map { if (it[K_CRK] != true) null else it[K_CR] }
    suspend fun setCrashOptIn(c: Context, v: Boolean) { c.ds.edit { it[K_CR] = v; it[K_CRK] = true } }
    suspend fun reviewCooldownOver(c: Context): Boolean =
        System.currentTimeMillis() - (c.ds.data.map { it[K_REV] ?: 0L }.first()) > 30L * DAY
    suspend fun markReviewAsked(c: Context) { c.ds.edit { it[K_REV] = System.currentTimeMillis() } }
    fun volumeFlow(c: Context): Flow<Float> = c.ds.data.map { it[K_VOL] ?: 1f }
    suspend fun setVolume(c: Context, v: Float) { c.ds.edit { it[K_VOL] = v } }
    fun hapticsFlow(c: Context): Flow<Boolean> = c.ds.data.map { it[K_HAP] ?: true }
    suspend fun setHaptics(c: Context, v: Boolean) { c.ds.edit { it[K_HAP] = v } }
    fun themeModeFlow(c: Context): Flow<ThemeMode> = c.ds.data.map {
        it[K_TH]?.let { v -> runCatching { ThemeMode.valueOf(v) }.getOrNull() } ?: ThemeMode.SYSTEM }
    suspend fun setThemeMode(c: Context, m: ThemeMode) { c.ds.edit { it[K_TH] = m.name } }
    fun dynamicColorFlow(c: Context): Flow<Boolean> = c.ds.data.map { it[K_DYN] ?: false }
    suspend fun setDynamicColor(c: Context, v: Boolean) { c.ds.edit { it[K_DYN] = v } }
    suspend fun resetAll(c: Context) = withContext(Dispatchers.IO) {
        ProgressDb.get(c).clearAllTables(); c.ds.edit { it.clear() } }
}
