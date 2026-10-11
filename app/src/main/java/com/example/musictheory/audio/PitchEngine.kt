package com.example.musictheory.audio
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log2
import kotlin.math.sqrt
object PitchDetector {
    const val WINDOW = 2048
    fun detect(samples: ShortArray, sampleRate: Int, threshold: Double = 0.15): Double? {
        val n = samples.size; if (n < WINDOW) return null
        val maxTau = WINDOW / 2; val d = DoubleArray(maxTau)
        for (tau in 1 until maxTau) { var s = 0.0
            for (i in 0 until WINDOW - maxTau) { val df = (samples[i] - samples[i + tau]).toDouble(); s += df * df }
            d[tau] = s }
        val cmnd = DoubleArray(maxTau); var run = 0.0; cmnd[0] = 1.0
        for (tau in 1 until maxTau) { run += d[tau]; cmnd[tau] = if (run > 0.0) d[tau] * tau / run else 1.0 }
        var best = -1; var tau = 1
        while (tau < maxTau) { if (cmnd[tau] < threshold) {
            while (tau + 1 < maxTau && cmnd[tau + 1] < cmnd[tau]) tau++; best = tau; break }; tau++ }
        if (best <= 0) return null
        val x0 = cmnd[best - 1]; val x1 = cmnd[best]; val x2 = if (best + 1 < maxTau) cmnd[best + 1] else x1
        val den = 2.0 * (2.0 * x1 - x0 - x2)
        val sh = if (den != 0.0) ((x2 - x0) / den).coerceIn(-1.0, 1.0) else 0.0
        val hz = sampleRate / (best + sh); return if (hz in 60.0..1600.0) hz else null }
    fun rms(s: ShortArray): Double { var a = 0.0; for (v in s) a += v.toDouble() * v; return sqrt(a / s.size) }
}
object PitchEngine {
    private const val SR = 44100
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _hz = MutableStateFlow<Double?>(null); val hz: StateFlow<Double?> = _hz.asStateFlow()
    private var job: Job? = null; private var record: AudioRecord? = null
    fun permissionGranted(c: Context) = ContextCompat.checkSelfPermission(c, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    fun start(c: Context): Boolean {
        if (job != null) return true; if (!permissionGranted(c)) return false
        val min = AudioRecord.getMinBufferSize(SR, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = AudioRecord(MediaRecorder.AudioSource.DEFAULT, SR, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, maxOf(min, PitchDetector.WINDOW * 4))
        if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); return false }
        record = rec; rec.startRecording(); _hz.value = null
        job = scope.launch { val b = ShortArray(PitchDetector.WINDOW)
            while (isActive) { val r = rec.read(b, 0, b.size)
                if (r > 0) _hz.value = if (PitchDetector.rms(b) < 250.0) null else PitchDetector.detect(b, SR) } }
        return true }
    fun stop() { job?.cancel(); job = null
        try { record?.stop() } catch (_: Exception) {}
        try { record?.release() } catch (_: Exception) {}
        record = null; _hz.value = null }
    fun semisOf(hz: Double) = 12.0 * log2(hz / TonePlayer.Notes.freqD(0.0))
    fun centsFrom(rootSemi: Double, hz: Double) = (semisOf(hz) - rootSemi) * 100.0
}
