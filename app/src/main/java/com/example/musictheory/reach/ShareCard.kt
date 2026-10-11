package com.example.musictheory.reach
import android.app.Activity; import android.content.Intent; import android.graphics.*
import android.graphics.Shader.TileMode.CLAMP
import androidx.core.content.FileProvider
import com.example.musictheory.data.*; import com.example.musictheory.util.safeStart
import kotlinx.coroutines.Dispatchers; import kotlinx.coroutines.flow.first; import kotlinx.coroutines.withContext
import java.io.File
object ShareCard {
    private data class D(val streak: Int, val studied: Int, val games: Int, val earned: Int)
    suspend fun buildAndShare(activity: Activity) { val ctx = activity.applicationContext
        val d = withContext(Dispatchers.IO) { val snap = AchSnap(ProgressRepo.studiedKeys(ctx).first(), emptySet(),
                ProgressRepo.gamesCount(ctx).first(), SettingsRepo.bests(ctx).first())
            D(SettingsRepo.streakFlow(ctx).first(), snap.studied.size, snap.gamesCount,
                Achievements.all.count { it.test(snap) }) }
        val file = withContext(Dispatchers.Default) { render(d, ctx) }
        withContext(Dispatchers.Main) { val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
            activity.safeStart(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share your progress"),
                "Nothing can share right now") } }
    private fun render(d: D, ctx: android.content.Context): File { val w = 1080; val h = 1350
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply {
            shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), 0xFF2A2270.toInt(), 0xFF4F46E5.toInt(), CLAMP) })
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val muted = Paint(white).apply { color = 0xFFB9B3FF.toInt() }
        val big = Paint(white).apply { textSize = 130f; isFakeBoldText = true }
        val mid = Paint(white).apply { textSize = 64f; isFakeBoldText = true }
        white.textSize = 88f
        c.drawText("🎵 Harmonia", 70f, 190f, mid)
        c.drawText("My music-theory streak", 70f, 320f, muted.apply { textSize = 44f })
        fun stat(x: Float, y: Float, e: String, v: String, l: String) { c.drawText(e, x, y, Paint(white).apply { textSize = 90f })
            c.drawText(v, x, y + 120f, big); c.drawText(l, x, y + 180f, muted.apply { textSize = 40f }) }
        stat(70f, 560f, "🔥", "${d.streak}", "day streak"); stat(560f, 560f, "📚", "${d.studied}", "topics studied")
        stat(70f, 950f, "🎮", "${d.games}", "games played"); stat(560f, 950f, "🏅", "${d.earned}", "achievements")
        c.drawText("Train your world-music ear too ↓", 70f, 1240f, muted.apply { textSize = 44f })
        val dir = File(ctx.cacheDir, "recordings").apply { mkdirs() }
        val f = File(dir, "harmonia_progress.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }; bmp.recycle(); return f }
}
