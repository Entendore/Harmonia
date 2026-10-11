package com.example.musictheory.audio
import android.content.Context
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
object WavRecorder {
    internal const val SR = 44100
    @Volatile internal var maxSamples: Int = SR * 180
    private var buf: ShortArray? = null
    private var len = 0
    @Volatile private var full = false
    val isRecording: Boolean @Synchronized get() = buf != null
    val isFull: Boolean @Synchronized get() = full
    @Synchronized fun recordedSeconds(): Int = len / SR
    @Synchronized fun lengthSamples(): Int = len
    @Synchronized fun start() { buf = ShortArray(1 shl 19); len = 0; full = false }
    @Synchronized fun add(data: ShortArray) { val b = buf ?: return; if (full) return
        val room = minOf(data.size, maxSamples - len)
        var target = b
        if (len + room > b.size) { target = b.copyOf(len + room); buf = target }
        System.arraycopy(data, 0, target, len, room); len += room
        if (len >= maxSamples) full = true }
    @Synchronized fun discard() { buf = null; len = 0; full = false }
    @Synchronized fun stop(context: Context): File? { val data = buf ?: return null; buf = null
        if (len == 0) return null
        val dir = File(context.cacheDir, "recordings").apply { mkdirs() }
        val f = File(dir, "harmonia_take_${System.currentTimeMillis()}.wav")
        DataOutputStream(BufferedOutputStream(FileOutputStream(f))).use { o ->
            val total = len * 2
            o.writeBytes("RIFF"); o.le(36 + total); o.writeBytes("WAVE"); o.writeBytes("fmt "); o.le(16)
            o.sle(1); o.sle(1); o.le(SR); o.le(SR * 2); o.sle(2); o.sle(16)
            o.writeBytes("data"); o.le(total)
            for (i in 0 until len) o.sle(data[i].toInt() and 0xFFFF) }
        return f }
    private fun DataOutputStream.le(v: Int) { write(v and 0xFF); write((v shr 8) and 0xFF)
        write((v shr 16) and 0xFF); write((v shr 24) and 0xFF) }
    private fun DataOutputStream.sle(v: Int) { write(v and 0xFF); write((v shr 8) and 0xFF) }
}
