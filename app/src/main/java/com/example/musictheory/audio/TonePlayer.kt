package com.example.musictheory.audio
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random
@kotlinx.serialization.Serializable enum class Timbre { SINE, PLUCK, BELL, DRUM }
object TonePlayer {
    private const val SR = 44100
    private val handler = Handler(Looper.getMainLooper())
    private val synthScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val active = mutableListOf<AudioTrack>()
    private const val MAX_ACTIVE = 10
    @Volatile private var generation = 0
    @Volatile var masterVolume: Float = 1f
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private val noopFocusListener = AudioManager.OnAudioFocusChangeListener { }
    fun attach(context: Context) {
        if (audioManager != null) return
        audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= 26) focusRequest = AudioFocusRequest.Builder(
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).build()
    }
    private fun ensureFocus() { val am = audioManager ?: return
        try { if (Build.VERSION.SDK_INT >= 26 && focusRequest != null) am.requestAudioFocus(focusRequest!!)
        else @Suppress("DEPRECATION") am.requestAudioFocus(noopFocusListener,
            AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) } catch (_: Exception) {} }
    private fun abandonIfIdle() {
        if (synchronized(active) { active.isNotEmpty() }) return
        val am = audioManager ?: return
        try { if (Build.VERSION.SDK_INT >= 26 && focusRequest != null) am.abandonAudioFocusRequest(focusRequest!!)
        else @Suppress("DEPRECATION") am.abandonAudioFocus(noopFocusListener) } catch (_: Exception) {} }
    fun playTone(freqHz: Double, durationMs: Int = 600, volume: Double = 0.8, timbre: Timbre = Timbre.SINE) {
        ensureFocus()
        val n = (SR.toLong() * durationMs / 1000L).toInt(); if (n <= 0) return
        val gen = generation; val vol = (volume * masterVolume).coerceIn(0.0, 1.0)
        synthScope.launch {
            val pcm = synthesizePcm(freqHz, n, timbre, vol)
            if (gen != generation) return@launch
            if (WavRecorder.isRecording) WavRecorder.add(pcm)
            val track = AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SR).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(n * 2).build()
            track.write(pcm, 0, n); track.play()
            synchronized(active) { active.add(track)
                if (active.size > MAX_ACTIVE) { val o = active.removeAt(0)
                    try { o.pause(); o.release() } catch (_: Exception) {} } }
            delay(durationMs + 150L)
            try { track.pause() } catch (_: Exception) {}
            try { track.release() } catch (_: Exception) {}
            synchronized(active) { active.remove(track) } }
        handler.postDelayed({ abandonIfIdle() }, durationMs + 300L)
    }
    internal fun synthesizePcm(freqHz: Double, n: Int, timbre: Timbre, volume: Double): ShortArray {
        val buf = FloatArray(n)
        when (timbre) {
            Timbre.SINE -> { val fade = (SR * 0.012).toInt().coerceAtMost(n / 2)
                val w = 2 * PI * freqHz / SR; var ph = 0.0
                for (i in 0 until n) { val env = if (i < fade) i / fade.toDouble()
                    else if (i >= n - fade) (n - i) / fade.toDouble() else 1.0
                    buf[i] = (sin(ph) * env).toFloat(); ph += w } }
            Timbre.PLUCK -> pluck(freqHz, buf)
            Timbre.BELL -> bell(freqHz, buf, n * 1000 / SR)
            Timbre.DRUM -> drum(buf) }
        val pcm = ShortArray(n)
        for (i in 0 until n) pcm[i] = ((buf[i] * volume).coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        return pcm
    }
    private fun pluck(freq: Double, buf: FloatArray) {
        val period = (SR / freq).toInt().coerceIn(2, SR / 40)
        val dl = DoubleArray(period) { Random.nextDouble(-1.0, 1.0) }; var ix = 0
        for (i in buf.indices) { val nx = (ix + 1) % period
            val v = 0.996 * 0.5 * (dl[ix] + dl[nx]); dl[ix] = v; buf[i] = v.toFloat(); ix = nx }
        val f = (SR * 0.004).toInt().coerceAtMost(buf.size / 4)
        for (i in 0 until f) { buf[i] *= i / f.toFloat(); buf[buf.size - 1 - i] *= i / f.toFloat() }
    }
    private fun bell(freq: Double, buf: FloatArray, durMs: Int) {
        val durS = durMs / 1000.0
        for ((ratio, amp) in listOf(1.0 to 1.0, 2.0 to 0.55, 2.76 to 0.45, 5.4 to 0.2, 8.93 to 0.1)) {
            val w = 2 * PI * freq * ratio / SR
            val k = Math.exp(-3.0 * (0.5 + ratio * 0.5) / (SR * durS)); var e = amp; var p = 0.0
            for (i in buf.indices) { buf[i] += (e * sin(p)).toFloat(); p += w; e *= k } }
        for (i in buf.indices) buf[i] *= 0.45f
    }
    private fun drum(buf: FloatArray) {
        val kf = Math.exp(-22.0 / SR); val kb = Math.exp(-7.0 / SR); val kn = Math.exp(-35.0 / SR)
        var f = 270.0; var body = 0.85; var noise = 0.35; var ph = 0.0
        for (i in buf.indices) { ph += 2 * PI * f / SR
            buf[i] = (body * sin(ph) + noise * Random.nextDouble(-1.0, 1.0)).toFloat()
            f *= kf; body *= kb; noise *= kn }
    }
    fun stopAll() { generation++; handler.removeCallbacksAndMessages(null)
        synchronized(active) { active.forEach { try { it.pause(); it.flush() } catch (_: Exception) {}
            try { it.release() } catch (_: Exception) {} }; active.clear() }
        abandonIfIdle()
    }
    fun playFrequencies(freqs: List<Double>, noteMs: Int = 500, gapMs: Long = 180, timbre: Timbre = Timbre.SINE) {
        var d = 0L; freqs.forEach { f -> handler.postDelayed({ playTone(f, noteMs, 0.8, timbre) }, d); d += noteMs + gapMs } }
    fun playChord(freqs: List<Double>, durationMs: Int = 900, timbre: Timbre = Timbre.SINE) {
        freqs.forEach { f -> handler.postDelayed({ playTone(f, durationMs, 0.7, timbre) }, 0L) } }
    fun playChordSequence(chords: List<List<Double>>, chordMs: Int = 750, gapMs: Long = 120, timbre: Timbre = Timbre.PLUCK) {
        var t = 0L; chords.forEach { c -> handler.postDelayed({ c.forEach { f -> playTone(f, chordMs, 0.7, timbre) } }, t); t += chordMs + gapMs } }
    fun playArpeggiatedProgression(chords: List<List<Double>>, noteMs: Int = 260, chordGapMs: Long = 140, timbre: Timbre = Timbre.PLUCK) {
        var t = 0L; chords.forEach { c -> c.forEach { f -> handler.postDelayed({ playTone(f, noteMs + 140, 0.7, timbre) }, t); t += noteMs }; t += chordGapMs } }
    fun playScale(semitones: List<Double>, noteMs: Int = 380, gapMs: Long = 90, withDrone: Boolean = false,
                  rootSemi: Double = 0.0, timbre: Timbre = Timbre.SINE) {
        stopAll()
        if (withDrone) { val dR = Notes.freqD(rootSemi - 12.0); val dF = Notes.freqD(rootSemi - 5.0)
            val total = semitones.size * (noteMs + gapMs) + 900L; var t = 0L
            while (t < total) { handler.postDelayed({ playTone(dR, 1100, 0.16) }, t)
                handler.postDelayed({ playTone(dF, 1100, 0.12) }, t + 70); t += 1000 } }
        playFrequencies(semitones.map { Notes.freqD(rootSemi + it) }, noteMs, gapMs, timbre)
    }
    fun playScaleLoop(semitones: List<Double>, noteMs: Int = 380, gapMs: Long = 90, withDrone: Boolean = false,
                      rootSemi: Double = 0.0, timbre: Timbre = Timbre.SINE) {
        stopAll(); val g = generation; val pass = semitones.size * (noteMs + gapMs) + 1200L
        synthScope.launch { while (g == generation && isActive) {
            if (withDrone) { val dR = Notes.freqD(rootSemi - 12.0); val dF = Notes.freqD(rootSemi - 5.0)
                var t = 0L; while (t < pass) { handler.postDelayed({ playTone(dR, 1100, 0.16) }, t)
                    handler.postDelayed({ playTone(dF, 1100, 0.12) }, t + 70); t += 1000 } }
            playFrequencies(semitones.map { Notes.freqD(rootSemi + it) }, noteMs, gapMs, timbre); delay(pass) } }
    }
    object Notes {
        private const val C4 = 261.6255653
        private val NAMES = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
        fun freqD(s: Double) = C4 * Math.pow(2.0, s / 12.0)
        fun freq(s: Int) = freqD(s.toDouble())
        fun name(s: Int): String { val i = ((s % 12) + 12) % 12
            return NAMES[i] + (4 + floor((s + 0.001) / 12.0).toInt()) }
        fun nameFractional(semi: Double): String { val fr = semi - floor(semi)
            val b = name(Math.ceil(semi).toInt()); return if (fr in 0.2..0.8) "$b half-flat" else b }
    }
}
