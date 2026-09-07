package com.buyzone.widget.domain

import kotlinx.serialization.Serializable

@Serializable
enum class CombinationMode { OR, AND }

@Serializable
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
        if (stage <= 0 || stageCount <= 0) return pack(45, 45, 52)
        if (stageCount == 1) return pack(orange[0], orange[1], orange[2])
        val t = (stage - 1).toDouble() / (stageCount - 1).toDouble()
        return if (t <= 0.5) interpolate(orange, red, t * 2.0) else interpolate(red, purple, (t - 0.5) * 2.0)
    }

    fun backgroundColor(stage: Int, stageCount: Int): Int {
        val accent = color(stage, stageCount)
        val base = intArrayOf(21, 25, 34)
        val tint = 0.22
        return pack(
            (base[0] + (redChannel(accent) - base[0]) * tint).toInt(),
            (base[1] + (greenChannel(accent) - base[1]) * tint).toInt(),
            (base[2] + (blueChannel(accent) - base[2]) * tint).toInt()
        )
    }

    private fun interpolate(a: IntArray, b: IntArray, t: Double): Int = pack(
        (a[0] + (b[0] - a[0]) * t).toInt(),
        (a[1] + (b[1] - a[1]) * t).toInt(),
        (a[2] + (b[2] - a[2]) * t).toInt()
    )

    private fun pack(red: Int, green: Int, blue: Int): Int =
        (0xFF shl 24) or (red.coerceIn(0, 255) shl 16) or
            (green.coerceIn(0, 255) shl 8) or blue.coerceIn(0, 255)

    private fun redChannel(color: Int) = color ushr 16 and 0xFF
    private fun greenChannel(color: Int) = color ushr 8 and 0xFF
    private fun blueChannel(color: Int) = color and 0xFF
}
