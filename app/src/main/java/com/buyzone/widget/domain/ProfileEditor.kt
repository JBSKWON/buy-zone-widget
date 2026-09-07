package com.buyzone.widget.domain

object ProfileEditor {
    fun removeIndicator(profile: TickerProfile, ruleId: String): TickerProfile =
        profile.copy(rules = profile.rules.filterNot { it.id == ruleId })

    fun setStageCount(profile: TickerProfile, stageCount: Int): TickerProfile {
        val count = stageCount.coerceIn(1, 7)
        return profile.copy(
            stageCount = count,
            rules = profile.rules.map { rule ->
                rule.copy(thresholds = resizeThresholds(rule.thresholds, count))
            }
        )
    }

    fun updateRule(profile: TickerProfile, rule: IndicatorRule): TickerProfile =
        profile.copy(rules = profile.rules.map { if (it.id == rule.id) rule else it })

    fun addIndicator(profile: TickerProfile, type: IndicatorType): TickerProfile {
        val number = profile.rules.count { it.type == type } + 1
        val rule = when (type) {
            IndicatorType.RSI -> IndicatorRule(
                id = "rsi-$number",
                type = type,
                timeframe = Timeframe.WEEKLY,
                comparison = Comparison.LESS_OR_EQUAL,
                thresholds = defaultThresholds(profile.stageCount, listOf(43.0, 36.0, 31.0)),
                period = 14
            )
            IndicatorType.SMA_DEVIATION -> IndicatorRule(
                id = "sma-deviation-$number",
                type = type,
                timeframe = Timeframe.DAILY,
                comparison = Comparison.LESS_OR_EQUAL,
                thresholds = defaultThresholds(profile.stageCount, listOf(-18.0, -32.0, -45.0)),
                period = 200
            )
            else -> IndicatorRule(
                id = "${type.name.lowercase()}-$number",
                type = type,
                timeframe = Timeframe.DAILY,
                comparison = Comparison.LESS_OR_EQUAL,
                thresholds = defaultThresholds(profile.stageCount, List(profile.stageCount) { 0.0 })
            )
        }
        return profile.copy(rules = profile.rules + rule)
    }

    private fun defaultThresholds(stageCount: Int, defaults: List<Double>): List<Double> {
        if (stageCount <= defaults.size) return defaults.take(stageCount)
        val last = defaults.lastOrNull() ?: 0.0
        return defaults + List(stageCount - defaults.size) { last }
    }

    private fun resizeThresholds(thresholds: List<Double>, stageCount: Int): List<Double> {
        if (thresholds.size >= stageCount) return thresholds.take(stageCount)
        val last = thresholds.lastOrNull() ?: 0.0
        return thresholds + List(stageCount - thresholds.size) { last }
    }
}
