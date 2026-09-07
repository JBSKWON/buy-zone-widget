package com.buyzone.widget.domain

import kotlin.math.pow
import kotlin.math.sqrt

enum class Timeframe { DAILY, WEEKLY }
enum class Comparison { LESS_OR_EQUAL, GREATER_OR_EQUAL }
enum class MovingAverage { SMA, EMA }
enum class IndicatorType { RSI, SMA_DEVIATION, EMA_DEVIATION, MA_SPREAD, MACD_HISTOGRAM, BOLLINGER_PERCENT_B }

data class PriceBar(val date: String, val close: Double)

data class IndicatorRule(
    val id: String,
    val type: IndicatorType,
    val timeframe: Timeframe,
    val comparison: Comparison,
    val thresholds: List<Double>,
    val period: Int = 14,
    val fastPeriod: Int = 12,
    val slowPeriod: Int = 26,
    val signalPeriod: Int = 9,
    val fastAverage: MovingAverage = MovingAverage.EMA,
    val slowAverage: MovingAverage = MovingAverage.EMA,
    val standardDeviationMultiplier: Double = 2.0
) {
    fun validate(stageCount: Int): List<String> {
        val errors = mutableListOf<String>()
        if (thresholds.size != stageCount) errors += "threshold count must equal stage count"
        if (period !in 2..250) errors += "period must be between 2 and 250"
        if (fastPeriod !in 2..250 || slowPeriod !in 2..250 || fastPeriod >= slowPeriod) {
            if (type == IndicatorType.MA_SPREAD || type == IndicatorType.MACD_HISTOGRAM) {
                errors += "fast and slow periods are invalid"
            }
        }
        if (signalPeriod !in 2..100) errors += "signal period must be between 2 and 100"
        if (standardDeviationMultiplier !in 0.1..10.0) errors += "multiplier must be between 0.1 and 10"
        val ordered = thresholds.zipWithNext().all { (a, b) ->
            if (comparison == Comparison.LESS_OR_EQUAL) a > b else a < b
        }
        if (!ordered && thresholds.size > 1) errors += "thresholds must become more severe by stage"
        return errors
    }
}

data class IndicatorSnapshot(
    val ruleId: String,
    val value: Double?,
    val marketDate: String?,
    val checkedAtEpochMs: Long,
    val error: String? = null
)

object IndicatorEngine {
    fun calculate(rule: IndicatorRule, bars: List<PriceBar>): Double? {
        val closes = bars.sortedBy { it.date }.map { it.close }
        if (closes.isEmpty()) return null
        return when (rule.type) {
            IndicatorType.RSI -> rsi(closes, rule.period)
            IndicatorType.SMA_DEVIATION -> deviation(closes, rule.period, MovingAverage.SMA)
            IndicatorType.EMA_DEVIATION -> deviation(closes, rule.period, MovingAverage.EMA)
            IndicatorType.MA_SPREAD -> {
                val fast = average(closes, rule.fastPeriod, rule.fastAverage)
                val slow = average(closes, rule.slowPeriod, rule.slowAverage)
                if (fast == null || slow == null || slow == 0.0) null else (fast / slow - 1.0) * 100.0
            }
            IndicatorType.MACD_HISTOGRAM -> macdHistogram(closes, rule.fastPeriod, rule.slowPeriod, rule.signalPeriod)
            IndicatorType.BOLLINGER_PERCENT_B -> bollingerPercentB(
                closes, rule.period, rule.standardDeviationMultiplier
            )
        }
    }

    private fun average(values: List<Double>, period: Int, type: MovingAverage): Double? {
        if (values.size < period) return null
        return when (type) {
            MovingAverage.SMA -> values.takeLast(period).average()
            MovingAverage.EMA -> emaSeries(values, period).lastOrNull()
        }
    }

    private fun rsi(values: List<Double>, period: Int): Double? {
        if (values.size <= period) return null
        var gains = 0.0
        var losses = 0.0
        for (i in 1..period) {
            val change = values[i] - values[i - 1]
            if (change >= 0) gains += change else losses -= change
        }
        var avgGain = gains / period
        var avgLoss = losses / period
        for (i in (period + 1) until values.size) {
            val change = values[i] - values[i - 1]
            val gain = change.coerceAtLeast(0.0)
            val loss = (-change).coerceAtLeast(0.0)
            avgGain = ((avgGain * (period - 1)) + gain) / period
            avgLoss = ((avgLoss * (period - 1)) + loss) / period
        }
        if (avgLoss == 0.0) return 100.0
        return 100.0 - (100.0 / (1.0 + avgGain / avgLoss))
    }

    private fun deviation(values: List<Double>, period: Int, type: MovingAverage): Double? {
        val price = values.lastOrNull() ?: return null
        val ma = average(values, period, type) ?: return null
        if (ma == 0.0) return null
        return (price / ma - 1.0) * 100.0
    }

    private fun emaSeries(values: List<Double>, period: Int): List<Double> {
        if (values.size < period) return emptyList()
        val multiplier = 2.0 / (period + 1)
        val result = ArrayList<Double>(values.size - period + 1)
        var previous = values.take(period).average()
        result += previous
        for (i in period until values.size) {
            previous = ((values[i] - previous) * multiplier) + previous
            result += previous
        }
        return result
    }

    private fun macdHistogram(values: List<Double>, fast: Int, slow: Int, signal: Int): Double? {
        if (values.size < slow + signal - 1) return null
        val fastSeries = emaSeries(values, fast)
        val slowSeries = emaSeries(values, slow)
        val offset = fastSeries.size - slowSeries.size
        val macd = slowSeries.indices.map { i -> fastSeries[i + offset] - slowSeries[i] }
        val signalSeries = emaSeries(macd, signal)
        if (signalSeries.isEmpty()) return null
        return macd.last() - signalSeries.last()
    }

    private fun bollingerPercentB(values: List<Double>, period: Int, multiplier: Double): Double? {
        if (values.size < period) return null
        val window = values.takeLast(period)
        val mean = window.average()
        val deviation = sqrt(window.sumOf { (it - mean).pow(2) } / period)
        val upper = mean + multiplier * deviation
        val lower = mean - multiplier * deviation
        return if (upper == lower) 0.5 else (values.last() - lower) / (upper - lower)
    }
}
