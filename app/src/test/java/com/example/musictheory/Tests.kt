package com.example.musictheory

import com.example.musictheory.audio.MidiWriter
import com.example.musictheory.audio.PitchDetector
import com.example.musictheory.audio.Timbre
import com.example.musictheory.audio.TonePlayer
import com.example.musictheory.audio.WavRecorder
import com.example.musictheory.data.ContentRepo
import com.example.musictheory.data.KeySigGen
import com.example.musictheory.data.LeitnerMath
import com.example.musictheory.data.MoreContent
import com.example.musictheory.data.StaffNote
import com.example.musictheory.data.db.LeitnerCard
import com.example.musictheory.util.RhythmGrid
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class NotesMathTest {
    @Test fun `frequency doubles every 12 semitones`() {
        val f0 = TonePlayer.Notes.freqD(0.0)
        assertEquals(2.0, TonePlayer.Notes.freqD(12.0) / f0, 1e-9) }
    @Test fun `names wrap octaves correctly`() {
        assertEquals("C4", TonePlayer.Notes.name(0))
        assertEquals("C5", TonePlayer.Notes.name(12))
        assertEquals("B3", TonePlayer.Notes.name(-1)) }
    @Test fun `fractional semitones are named half-flat`() {
        assertTrue(TonePlayer.Notes.nameFractional(3.5).contains("half-flat")) }
}
class LeitnerMathTest {
    private fun card(box: Int = 0) = LeitnerCard("r", box, 0, 0)
    @Test fun `correct moves up a box`() { val g = LeitnerMath.graded(card(2), true, 1000)
        assertEquals(3, g.box); assertEquals(1000 + LeitnerMath.DELAYS[3], g.dueAt) }
    @Test fun `wrong resets to box 0`() { assertEquals(0, LeitnerMath.graded(card(4), false, 1000).box) }
    @Test fun `box caps at last delay`() { assertEquals(LeitnerMath.DELAYS.lastIndex, LeitnerMath.graded(card(5), true, 0).box) }
}
class RhythmGridTest {
    @Test fun `absolute schedule cancels cumulative jitter`() {
        var fake = 0L; val fired = mutableListOf<Long>()
        runBlocking { RhythmGrid.run(16, 2, 100, 0, { fake }, { fake += it + 7 }, { fired.add(fake) }) }
        fired.forEachIndexed { i, t -> assertTrue("step $i at $t", abs(t - i * 100) <= 7) } }
}
class PitchDetectorTest {
    private fun sine(hz: Double, n: Int = PitchDetector.WINDOW) =
        ShortArray(n) { (sin(2 * PI * hz * it / 44100.0) * 12000).toInt().toShort() }
    @Test fun `detects 220 Hz within two Hz`() {
        assertTrue(abs(PitchDetector.detect(sine(220.0), 44100)!! - 220.0) < 2.0) }
    @Test fun `detects 440 Hz`() {
        assertTrue(abs(PitchDetector.detect(sine(440.0), 44100)!! - 440.0) < 3.0) }
    @Test fun `silence returns null`() {
        assertNull(PitchDetector.detect(ShortArray(PitchDetector.WINDOW), 44100)) }
    @Test fun `octave ambiguity resolves to fundamental`() {
        val mixed = ShortArray(PitchDetector.WINDOW) {
            ((sin(2 * PI * 220 * it / 44100.0) * 12000 +
              sin(2 * PI * 440 * it / 44100.0) * 3000)).toInt().toShort() }
        assertTrue(abs(PitchDetector.detect(mixed, 44100)!! - 220.0) < 4.0) }
}
class SynthTest {
    @Test fun `all timbres produce audible unclipped buffers`() { Timbre.values().forEach { t ->
        val pcm = TonePlayer.synthesizePcm(440.0, 22050, t, 0.8)
        assertEquals(22050, pcm.size)
        val peak = pcm.maxOf { abs(it.toInt()) }
        assertTrue("timbre $t silent", peak > 1000)
        assertTrue("timbre $t clips", peak <= Short.MAX_VALUE.toInt()) } }
    @Test fun `pluck decays`() { val pcm = TonePlayer.synthesizePcm(220.0, 22050, Timbre.PLUCK, 1.0)
        fun rms(r: IntRange) = sqrt((r.first..r.last).map {
            (pcm[it].toDouble() / 32767).pow(2) }.average())
        assertTrue(rms(0..5500) > rms(16550..22049) * 1.5) }
}
class WavRecorderTest {
    @Test fun `cap stops accumulation`() { WavRecorder.maxSamples = 1000; WavRecorder.start()
        WavRecorder.add(ShortArray(700)); WavRecorder.add(ShortArray(700))
        assertTrue(WavRecorder.isFull); assertEquals(1000, WavRecorder.lengthSamples())
        WavRecorder.add(ShortArray(500)); assertEquals(1000, WavRecorder.lengthSamples())
        WavRecorder.discard() }
    @Test fun `adds without start are no-ops`() { WavRecorder.discard()
        WavRecorder.add(ShortArray(100)); assertEquals(0, WavRecorder.lengthSamples()) }
}
class KeySigGenTest {
    @Test fun `answer always in range and options unique`() { repeat(300) { n ->
        val q = KeySigGen.generate(n)
        assertTrue(q.answer in q.options.indices)
        assertEquals(q.options.size, q.options.distinct().size) } }
}
class StaffNoteTest {
    @Test fun `ledgers never land on staff lines`() { StaffNote.tiers.forEach { spec ->
        spec.range.forEach { d -> StaffNote.ledgers(spec, d).forEach { ld ->
            assertTrue("ledger $ld inside staff (d=$d)", ld < spec.bottomLine || ld > spec.bottomLine + 8) } } } }
    @Test fun `space above staff gets no phantom ledger`() {
        assertTrue(StaffNote.ledgers(StaffNote.tiers[1], 11).isEmpty())
        assertEquals(listOf(12), StaffNote.ledgers(StaffNote.tiers[1], 12)) }
}
class MidiWriterTest {
    @Test fun `header is valid format-0`() {
        val b = MidiWriter.write(listOf(MidiWriter.Note(60, 0, 120)), 120)
        assertEquals("MThd", String(b.copyOfRange(0, 4), Charsets.US_ASCII)) }
    @Test fun `note-off before same-tick note-on`() {
        val b = MidiWriter.write(listOf(MidiWriter.Note(60, 0, 240), MidiWriter.Note(64, 240, 120)), 120)
        val s = b.joinToString(",") { "%02X".format(it) }
        assertTrue(s.indexOf("80,40") < s.indexOf("90,40")) }
}
class ContentValidationTest {
    companion object {
        @JvmStatic @BeforeClass fun load() {
            if (ContentRepo.isLoaded) return
            val cwd = File(System.getProperty("user.dir"))
            val module = sequenceOf(cwd, cwd.resolve("app"))
                .first { File(it, "src/main/assets").exists() }
            val assets = File(module, "src/main/assets/content")
            ContentRepo.load { rel -> File(assets, rel.removePrefix("content/")).inputStream() } } }
    @Test fun `loader validation passed`() = assertTrue(ContentRepo.isLoaded)
    @Test fun `categories partition the bank`() = assertEquals(ContentRepo.questions.size,
        ContentRepo.questionsByCategory("western").size +
        ContentRepo.questionsByCategory("eastern").size +
        ContentRepo.questionsByCategory("world").size)
    @Test fun `rhythm tag is a healthy subset`() =
        assertTrue(ContentRepo.questionsByTag("rhythm").size >= 10)
    @Test fun `quiz-me covers most topics`() {
        val covered = ContentRepo.topics.count { ContentRepo.questionsForTopic(it.id).isNotEmpty() }
        assertTrue("only $covered topics covered", covered >= 30) }
    @Test fun `match bank builds playable rounds`() { repeat(5) {
        val set = MoreContent.allMatchQuestions()
        assertEquals(10, set.size)
        set.forEach { q -> assertTrue(q.answer in q.options.indices) } } }
}
