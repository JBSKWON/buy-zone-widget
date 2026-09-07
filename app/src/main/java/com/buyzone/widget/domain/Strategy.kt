package com.buyzone.widget.domain

import android.graphics.Color

enum class CombinationMode { OR, AND }

data class TickerProfile(
    val ticker: String,
    val stageCount: Int = 3,
    val combinationMode: CombinationMode = CombinationMode.OR,
    val rules: List<IndicatorRule> = emptyList()
) {
    fun validate(): List<String> = buildList {
        if (ticker.isBlank()) add("ticker is required")
        if (stageCount !in 1..7) add("stage count must be between 1 and 7")
        rules.forEach { addAll(it.validate(stageCount)) }
    }
}

object StrategyEvaluator {
    fun stage(profile: TickerProfile, snapshots: List<IndicatorSnapshot>): Int? {
        if (profile.rules.isEmpty() || profile.validate().isNotEmpty()) return null
        val stages = profile.rules.mapNotNull { rule ->
            snapshots.firstOrNull { it.ruleId == rule.id }?.value?.let { value ->
                val deepestMatch = when (rule.comparison) {
                    Comparison.LESS_OR_EQUAL -> rule.thresholds.indexOfLast { value <= it }
                    Comparison.GREATER_OR_EQUAL -> rule.thresholds.indexOfLast { value >= it }
                }
                if (deepestMatch < 0) 0 else deepestMatch + 1
            }
        }
        if (stages.isEmpty()) return null
        return when (profile.combinationMode) {
            CombinationMode.OR -> stages.maxOrNull()
            CombinationMode.AND -> stages.minOrNull()
        }?.coerceIn(0, profile.stageCount)
    }
}

object StageColors {
    private val orange = intArrayOf(245, 158, 11)
    private val red = intArrayOf(239, 68, 68)
    private val purple = intArrayOf(139, 92, 246)

    fun color(stage: Int, stageCount: Int): Int {
        if (stage <= 0 || stageCount <= 0) return Color.rgb(45, 45, 52)
        if (stageCount == 1) return Color.rgb(orange[0], orange[1], orange[2])
        val t = (stage - 1).toDouble() / (stageCount - 1).toDouble()
        return if (t <= 0.5) interpolate(orange, red, t * 2.0) else interpolate(red, purple, (t - 0.5) * 2.0)
    }

    private fun interpolate(a: IntArray, b: IntArray, t: Double): Int = Color.rgb(
        (a[0] + (b[0] - a[0]) * t).toInt(),
        (a[1] + (b[1] - a[1]) * t).toInt(),
        (a[2] + (b[2] - a[2]) * t).toInt()
    )
}
