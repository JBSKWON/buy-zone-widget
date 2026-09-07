package com.buyzone.widget.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileEditorTest {
    @Test
    fun addsTheOriginalWeeklyRsiRuleToAProfile() {
        val profile = ProfileEditor.addIndicator(TickerProfile("TQQQ"), IndicatorType.RSI)

        assertEquals(1, profile.rules.size)
        assertEquals(IndicatorType.RSI, profile.rules.single().type)
        assertEquals(Timeframe.WEEKLY, profile.rules.single().timeframe)
        assertEquals(listOf(43.0, 36.0, 31.0), profile.rules.single().thresholds)
    }

    @Test
    fun addsTheOriginalDailyDeviationRuleWithoutRemovingExistingRules() {
        val first = ProfileEditor.addIndicator(TickerProfile("TQQQ"), IndicatorType.RSI)
        val second = ProfileEditor.addIndicator(first, IndicatorType.SMA_DEVIATION)

        assertEquals(2, second.rules.size)
        assertTrue(second.rules.any { it.type == IndicatorType.SMA_DEVIATION && it.timeframe == Timeframe.DAILY })
    }
}
