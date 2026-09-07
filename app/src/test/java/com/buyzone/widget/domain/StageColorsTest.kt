package com.buyzone.widget.domain

import org.junit.Assert.assertTrue
import org.junit.Test

class StageColorsTest {
    @Test
    fun stageBackgroundIsDarkerThanItsAccent() {
        val accent = StageColors.color(2, 3)
        val background = StageColors.backgroundColor(2, 3)

        assertTrue(red(background) < red(accent))
        assertTrue(green(background) < green(accent))
        assertTrue(blue(background) < blue(accent))
    }

    private fun red(color: Int) = color ushr 16 and 0xFF
    private fun green(color: Int) = color ushr 8 and 0xFF
    private fun blue(color: Int) = color and 0xFF
}
