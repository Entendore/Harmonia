package com.example.musictheory.data
import com.example.musictheory.data.db.LeitnerCard
object LeitnerMath {
    const val MIN = 60_000L; const val DAY = 24 * 60 * MIN
    val DELAYS = longArrayOf(10 * MIN, DAY, 2 * DAY, 4 * DAY, 8 * DAY, 16 * DAY)
    fun graded(card: LeitnerCard, correct: Boolean, now: Long): LeitnerCard {
        val box = if (correct) (card.box + 1).coerceAtMost(DELAYS.lastIndex) else 0
        return LeitnerCard(card.ref, box, now + DELAYS[box], card.seen + 1) }
}
