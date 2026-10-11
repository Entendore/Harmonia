package com.example.musictheory.data
object StaffNote {
    const val LETTERS = "CDEFGAB"
    fun name(d: Int) = LETTERS[(((d % 7) + 7) % 7)].toString()
    data class Spec(val bass: Boolean, val range: List<Int>, val bottomLine: Int,
        val ledgerBelow: Int?, val ledgerAbove: Int?)
    val tiers = listOf(
        Spec(false, (2..10).toList(), 2, null, null),
        Spec(false, (0..12).toList(), 2, 0, 12),
        Spec(true, (-10..-2).toList(), -10, null, null),
        Spec(true, (-12..0).toList(), -10, -12, 0))
    fun ledgers(spec: Spec, d: Int): List<Int> = buildList {
        spec.ledgerBelow?.let { lb -> if (d <= lb) for (x in lb downTo d step 2) add(x) }
        spec.ledgerAbove?.let { la -> if (d >= la) for (x in la..d step 2) add(x) } }
}
