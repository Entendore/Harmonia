package com.example.musictheory.ui.common
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
@Composable
fun RhythmWheel(slots: Int, accents: Set<Int>, progressProvider: () -> Float, taps: List<Float> = emptyList(),
                tapTolerance: Float = 0.03f, slotStates: List<Int>? = null, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme; val okC = correctColor(); val badC = wrongColor()
    Box(modifier.aspectRatio(1f).semantics {
        contentDescription = "Rhythm wheel with $slots steps, ${accents.size} accented" }) {
        Canvas(Modifier.fillMaxSize()) {
            val progress = progressProvider()
            val r = size.minDimension / 2f * 0.92f; val c = Offset(size.width / 2f, size.height / 2f)
            fun ang(t: Float) = t * 2f * PI.toFloat() - PI.toFloat() / 2f
            fun pt(t: Float, rad: Float) = c + Offset(cos(ang(t)), sin(ang(t))) * rad
            drawCircle(cs.outline.copy(alpha = 0.35f), r, c, style = Stroke(3f))
            for (i in 0 until slots) { val st = slotStates?.getOrNull(i) ?: if (i in accents) 2 else 1
                val (col, rad) = when (st) { 2 -> cs.primary to 16f
                    1 -> cs.secondary.copy(alpha = 0.9f) to 10f; else -> cs.outline.copy(alpha = 0.5f) to 5f }
                drawCircle(col, rad, pt(i.toFloat() / slots, r))
                if (st == 2) drawCircle(cs.primary.copy(alpha = 0.22f), 26f, pt(i.toFloat() / slots, r)) }
            taps.forEach { t -> val near = accents.any { a ->
                val d = abs(t - a.toFloat() / slots); min(d, 1f - d) <= tapTolerance }
                drawCircle(if (near) okC else badC, 8f, pt(t, r * 1.09f)) }
            val dir = Offset(cos(ang(progress)), sin(ang(progress)))
            drawLine(cs.tertiary, c + dir * (r * 0.15f), c + dir * (r * 1.05f), 5f, StrokeCap.Round)
            drawCircle(cs.tertiary, 9f, pt(progress, r * 1.05f)) } }
}
