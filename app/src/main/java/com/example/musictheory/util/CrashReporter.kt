package com.example.musictheory.util
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File
import java.util.Date
object CrashReporter {
    fun install(context: Context) { val dir = File(context.filesDir, "crash")
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e -> try { dir.mkdirs()
            File(dir, "last.txt").writeText("${Date()}\n${e.stackTraceToString().take(6000)}") } catch (_: Throwable) {}
            prev?.uncaughtException(t, e) } }
    fun pending(context: Context): String? =
        File(context.filesDir, "crash/last.txt").takeIf { it.exists() }?.readText()?.take(4000)
    fun clear(context: Context) { File(context.filesDir, "crash/last.txt").delete() }
    fun sendMail(context: Context, trace: String) { context.safeStart(Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:harmonia.crashes@example.com")
        putExtra(Intent.EXTRA_SUBJECT, "Harmonia crash report"); putExtra(Intent.EXTRA_TEXT, trace) },
        "No email app found") }
}
