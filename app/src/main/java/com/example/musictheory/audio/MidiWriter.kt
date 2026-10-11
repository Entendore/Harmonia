package com.example.musictheory.audio
import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
object MidiWriter {
    data class Note(val midi: Int, val startTick: Int, val durTicks: Int, val velocity: Int = 100)
    fun write(notes: List<Note>, tempoBpm: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.write("MThd".toByteArray()); le(out, 6); se(out, 0); se(out, 1); se(out, 480)
        out.write("MTrk".toByteArray())
        val t = ByteArrayOutputStream()
        val mpq = 60_000_000 / tempoBpm.coerceIn(20, 300)
        t.write(byteArrayOf(0, 0xFF.toByte(), 0x51, 0x03, ((mpq shr 16) and 0xFF).toByte(),
            ((mpq shr 8) and 0xFF).toByte(), (mpq and 0xFF).toByte()))
        data class Ev(val tick: Int, val b: ByteArray, val ord: Int)
        val evs = buildList { notes.forEach { n ->
            add(Ev(n.startTick, byteArrayOf(0x90, n.midi.toByte(), n.velocity.toByte()), 0))
            add(Ev(n.startTick + n.durTicks, byteArrayOf(0x80, n.midi.toByte(), 0), 1)) } }
            .sortedWith(compareBy({ it.tick }, { it.ord }))
        var last = 0
        evs.forEach { vlq(t, it.tick - last); last = it.tick; t.write(it.b) }
        t.write(byteArrayOf(0, 0xFF.toByte(), 0x2F, 0x00))
        val tb = t.toByteArray(); le(out, tb.size); out.write(tb); return out.toByteArray() }
    fun exportBytes(context: Context, notes: List<Note>, bpm: Int): Uri? {
        val dir = File(context.cacheDir, "recordings").apply { mkdirs() }
        val f = File(dir, "harmonia_${System.currentTimeMillis()}.mid"); f.writeBytes(write(notes, bpm))
        return androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", f) }
    private fun le(o: ByteArrayOutputStream, v: Int) { o.write(byteArrayOf(((v shr 24) and 0xFF).toByte(),
        ((v shr 16) and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte(), (v and 0xFF).toByte())) }
    private fun se(o: ByteArrayOutputStream, v: Int) = o.write(byteArrayOf(((v shr 8) and 0xFF).toByte(), (v and 0xFF).toByte()))
    private fun vlq(o: ByteArrayOutputStream, v0: Int) { var v = v0; val bs = mutableListOf<Int>()
        bs.add(v and 0x7F); v = v shr 7
        while (v > 0) { bs.add((v and 0x7F) or 0x80); v = v shr 7 }
        bs.reversed().forEach { o.write(it) } }
}
