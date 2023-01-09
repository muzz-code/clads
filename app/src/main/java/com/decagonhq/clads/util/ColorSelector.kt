package com.decagonhq.clads.util

object ColorSelector {
    private fun colors(): List<Int> =
        listOf(
            -0xf44336,
            -0xf9d6e,
            -0x459738,
            -0x6a8a33,
            -0x867935,
            -0x9b4a0a,
            -0xb03c09,
            -0xb22f1f,
            -0xb24954,
            -0x7e387c,
            -0x512a7f,
            -0x759b,
            -0x2b1ea9,
            -0x2ab1,
            -0x48b3,
            -0x5e7781,
            -0x6f5b52,
            -0x459738,
            -0x6a8a33,
            -0x867935,
            -0x512a7f,
            -0x759b,
            -0x2b1ea9,
            -0x759b,
            -0x2b1ea9,
            -0x9b4a0a
        )

    fun selectColorByCharacter(char: Char): Int {
        val colors = colors()
        val charArray = (65..90).map {
            it.toChar()
        }
        return colors[charArray.indexOf(char)]
    }
}
