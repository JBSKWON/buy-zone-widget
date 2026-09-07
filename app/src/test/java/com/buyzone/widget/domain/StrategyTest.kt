package com.buyzone.widget.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StrategyTest {
    private val rsi = IndicatorRule(
        id = "rsi", type = IndicatorType.RSI, timeframe = Timeframe.WEEKLY,
        comparison = Comparison.LESS_OR_EQUAL, thresholds = listOf(43.0, 36.0, 31.0)
    )

    @Test
    fun orModeUsesStrongestReachedStage() {
        val profile = TickerProfile("TQQQ", 3, CombinationMode.OR, listOf(rsi))
        val snapshots = listOf(IndicatorSnapshot("rsi", 30.0, "2026-01-01", 1L))

        assertEquals(3, StrategyEvaluator.stage(profile, snapshots))
    }

    @Test
    fun andModeUsesWeakestReachedStage() {
        val second = rsi.copy(
            id = "deviation",
            type = IndicatorType.SMA_DEVIATION,
            thresholds = listOf(-18.0, -32.0, -45.0)
        )
        val profile = TickerProfile("TQQQ", 3, CombinationMode.AND, listOf(rsi, second))
        val snapshots = listOf(
            IndicatorSnapshot("rsi", 30.0, "2026-01-01", 1L),
            IndicatorSnapshot("deviation", -20.0, "2026-01-01", 1L)
        )

        assertEquals(1, StrategyEvaluator.stage(profile, snapshots))
    }
}
