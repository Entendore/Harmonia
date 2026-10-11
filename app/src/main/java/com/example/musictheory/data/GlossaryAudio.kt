package com.example.musictheory.data
object GlossaryAudio {
    data class Clip(val semis: List<Double>, val drone: Boolean = false, val chord: Boolean = false)
    val map = mapOf(
        "Rast" to Clip(listOf(0.0,2.0,3.5,5.0,7.0,9.0,10.5), drone = true),
        "Bayati" to Clip(listOf(0.0,1.5,3.0,5.0,7.0,8.0,10.0), drone = true),
        "Half-flat" to Clip(listOf(0.0,1.5,3.0), drone = true),
        "Quarter tone" to Clip(listOf(0.0,0.5,1.0), drone = true),
        "Microtone" to Clip(listOf(0.0,0.5,1.0), drone = true),
        "Maqam" to Clip(listOf(0.0,2.0,3.5,5.0,7.0,9.0,10.5), drone = true),
        "Hicaz" to Clip(listOf(0.0,1.0,4.0,5.0,7.0,8.0,10.0), drone = true),
        "Tetrachord" to Clip(listOf(0.0,1.5,3.0,5.0), drone = true),
        "Slendro" to Clip(listOf(0.0,2.4,4.8,7.2,9.6)),
        "Pelog" to Clip(listOf(0.0,1.2,3.4,5.6,7.6)),
        "Wuyin" to Clip(listOf(0.0,2.0,4.0,7.0,9.0)),
        "Yo" to Clip(listOf(0.0,2.0,5.0,7.0,9.0)),
        "Pentatonic" to Clip(listOf(0.0,2.0,4.0,7.0,9.0)),
        "Ionian" to Clip(listOf(0.0,2.0,4.0,5.0,7.0,9.0,11.0)),
        "Dorian" to Clip(listOf(0.0,2.0,3.0,5.0,7.0,9.0,10.0)),
        "Phrygian" to Clip(listOf(0.0,1.0,3.0,5.0,7.0,8.0,10.0)),
        "Lydian" to Clip(listOf(0.0,2.0,4.0,6.0,7.0,9.0,11.0)),
        "Mixolydian" to Clip(listOf(0.0,2.0,4.0,5.0,7.0,9.0,10.0)),
        "Drone" to Clip(listOf(0.0,7.0,12.0), chord = true),
        "Blue note" to Clip(listOf(0.0,3.0,5.0,6.0,7.0,10.0)),
        "Tritone" to Clip(listOf(0.0,6.0)))
}
