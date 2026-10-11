package com.example.musictheory.data.db
import android.content.Context; import androidx.room.*; import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase; import kotlinx.coroutines.flow.Flow
@Entity(tableName = "topic_progress")
data class TopicProgress(@PrimaryKey val topicKey: String, val title: String,
    val readCount: Int, val demoPlays: Int, val lastOpened: Long)
@Entity(tableName = "game_results", indices = [Index("playedAt")])
data class GameResult(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val game: String, val score: Int, val playedAt: Long)
@Entity(tableName = "user_patterns")
data class UserPattern(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String,
    val slots: Int, val slotMs: Int, val pattern: String, val createdAt: Long)
@Entity(tableName = "leitner_cards", indices = [Index("dueAt")])
data class LeitnerCard(@PrimaryKey val ref: String, val box: Int, val dueAt: Long, val seen: Int)
@Entity(tableName = "answer_stats", indices = [Index(value = ["game", "playedAt"])])
data class AnswerStat(@PrimaryKey(autoGenerate = true) val id: Long = 0, val game: String,
    val item: String, val correct: Int, val playedAt: Long)
@Entity(tableName = "path_progress")
data class PathProgress(@PrimaryKey val pathId: String, val completedSteps: Int, val completedAt: Long)
@Dao interface ProgressDao {
    @Query("SELECT * FROM topic_progress ORDER BY lastOpened DESC") fun allTopics(): Flow<List<TopicProgress>>
    @Query("SELECT COUNT(*) FROM topic_progress WHERE readCount > 0") fun topicsStudied(): Flow<Int>
    @Query("SELECT * FROM topic_progress WHERE topicKey = :k") suspend fun topic(k: String): TopicProgress?
    @Upsert suspend fun upsertTopic(p: TopicProgress)
    @Insert suspend fun insertResult(r: GameResult)
    @Query("SELECT COUNT(*) FROM game_results") fun gamesPlayed(): Flow<Int>
    @Query("SELECT DISTINCT game FROM game_results") fun playedGames(): Flow<List<String>>
    @Query("SELECT * FROM game_results ORDER BY playedAt DESC") fun allResults(): Flow<List<GameResult>>
    @Insert suspend fun insertPattern(p: UserPattern): Long
    @Query("SELECT * FROM user_patterns ORDER BY createdAt DESC") fun allPatterns(): Flow<List<UserPattern>>
    @Delete suspend fun deletePattern(p: UserPattern)
    @Insert suspend fun insertAnswer(a: AnswerStat)
    @Query("SELECT * FROM answer_stats WHERE game = :g ORDER BY playedAt DESC LIMIT 20")
    fun recentAnswers(g: String): Flow<List<AnswerStat>>
    @Query("SELECT COUNT(*) FROM answer_stats WHERE game = :g") suspend fun answerCount(g: String): Int
    @Query("SELECT * FROM leitner_cards WHERE dueAt <= :now ORDER BY dueAt LIMIT :n")
    suspend fun due(now: Long, n: Int): List<LeitnerCard>
    @Query("SELECT ref FROM leitner_cards") suspend fun allRefs(): List<String>
    @Query("SELECT * FROM leitner_cards") suspend fun allCardsOnce(): List<LeitnerCard>
    @Query("SELECT * FROM leitner_cards WHERE ref = :r") suspend fun byRef(r: String): LeitnerCard?
    @Upsert suspend fun upsertCard(c: LeitnerCard)
    @Query("DELETE FROM leitner_cards WHERE ref = :r") suspend fun deleteCard(r: String)
    @Query("SELECT * FROM path_progress") fun allPathProgress(): Flow<List<PathProgress>>
    @Query("SELECT * FROM path_progress") suspend fun allPathProgressOnce(): List<PathProgress>
    @Query("SELECT * FROM path_progress WHERE pathId = :id") suspend fun pathProgress(id: String): PathProgress?
    @Upsert suspend fun upsertPathProgress(p: PathProgress) }
@Database(entities = [TopicProgress::class, GameResult::class, UserPattern::class,
    LeitnerCard::class, AnswerStat::class, PathProgress::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() { abstract fun dao(): ProgressDao }
object ProgressDb {
    @Volatile private var instance: AppDatabase? = null
    private val M12 = object : Migration(1, 2) { override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS leitner_cards (ref TEXT NOT NULL PRIMARY KEY, box INTEGER NOT NULL, dueAt INTEGER NOT NULL, seen INTEGER NOT NULL)") } }
    private val M23 = object : Migration(2, 3) { override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS path_progress (pathId TEXT NOT NULL PRIMARY KEY, completedSteps INTEGER NOT NULL, completedAt INTEGER NOT NULL)") } }
    private val M34 = object : Migration(3, 4) { override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS answer_stats (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, game TEXT NOT NULL, item TEXT NOT NULL, correct INTEGER NOT NULL, playedAt INTEGER NOT NULL)") } }
    private val M45 = object : Migration(4, 5) { override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_game_results_playedAt ON game_results (playedAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_answer_stats_game_playedAt ON answer_stats (game, playedAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_leitner_cards_dueAt ON leitner_cards (dueAt)") } }
    fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "harmonia.db")
            .addMigrations(M12, M23, M34, M45).fallbackToDestructiveMigration()
            .build().also { instance = it } }
}
