package com.example.musictheory.data
import com.example.musictheory.model.Question; import kotlin.random.Random
object KeySigGen {
    private val sharpKeys = listOf("C" to 0,"G" to 1,"D" to 2,"A" to 3,"E" to 4,"B" to 5,"F♯" to 6,"C♯" to 7)
    private val flatKeys = listOf("F" to 1,"B♭" to 2,"E♭" to 3,"A♭" to 4,"D♭" to 5,"G♭" to 6,"C♭" to 7)
    private val all = sharpKeys + flatKeys; private val sharpNames = sharpKeys.map { it.first }.toSet()
    fun generate(n: Int, rnd: Random = Random): Question {
        val (key, count) = all.random(rnd)
        return if (n % 2 == 0) { val sym = if (key in sharpNames) "♯" else "♭"
            val correct = "$count $sym"
            val final = (((0..7).toList() - count).shuffled(rnd).take(3).map { "$it $sym" } + correct).shuffled(rnd)
            Question("How many ♯/♭ in $key major?", final, final.indexOf(correct),
                "$key major carries $count accidental${if (count == 1) "" else "s"}.") }
        else { val sym = if (key.endsWith("♯")) "♯" else "♭"
            val final = (all.filter { it.first != key }.shuffled(rnd).take(3).map { it.first } + key).shuffled(rnd)
            Question("Which major key has $count $sym?", final, final.indexOf(key),
                "$count $sym points to $key major on the Circle of Fifths.") } }
}
