package com.buyzone.widget.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IndicatorEngineTest {
    @Test
    fun rsiUsesWilderSmoothingAndReturnsExtremeForUninterruptedRise() {
        val bars = (0..20).map { PriceBar("2026-01-${(it + 1).toString().padStart(2, '0')}", 100.0 + it) }
        val rule = IndicatorRule("rsi", IndicatorType.RSI, Timeframe.WEEKLY, Comparison.LESS_OR_EQUAL, listOf(43.0), period = 14)

        assertEquals(100.0, IndicatorEngine.calculate(rule, bars)!!, 0.0001)
    }

    @Test
    fun smaDeviationIsPercentageFromTheMovingAverage() {
        val bars = (1..5).map { PriceBar("2026-01-$it", it.toDouble()) }
        val rule = IndicatorRule("dev", IndicatorType.SMA_DEVIATION, Timeframe.DAILY, Comparison.LESS_OR_EQUAL, listOf(-18.0), period = 5)

        assertEquals(66.6667, IndicatorEngine.calculate(rule, bars)!!, 0.0001)
    }

    @Test
    fun invalidRuleRejectsNonDescendingLowerThresholds() {
        val rule = IndicatorRule("rsi", IndicatorType.RSI, Timeframe.WEEKLY, Comparison.LESS_OR_EQUAL, listOf(31.0, 36.0, 43.0))

        assertTrue(rule.validate(3).any { it.contains("thresholds") })
    }
}
