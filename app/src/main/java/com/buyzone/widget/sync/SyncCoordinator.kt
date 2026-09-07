package com.buyzone.widget.sync

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll
import com.buyzone.widget.BuyZoneApplication
import com.buyzone.widget.data.TickerSnapshot
import com.buyzone.widget.domain.IndicatorEngine
import com.buyzone.widget.domain.IndicatorSnapshot
import com.buyzone.widget.domain.Timeframe
import com.buyzone.widget.widget.BuyZoneWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate

class SyncCoordinator(private val context: Context) {
    private val container get() = (context.applicationContext as BuyZoneApplication).container

    suspend fun sync(ticker: String?): SyncResult = withContext(Dispatchers.IO) {
        val profiles = if (ticker == null) container.profileStore.all() else {
            listOfNotNull(container.profileStore.get(ticker))
        }
        if (profiles.isEmpty()) return@withContext SyncResult.NothingToDo
        val token = container.tokenStore.read() ?: return@withContext SyncResult.MissingToken
        var result: SyncResult = SyncResult.Success
        profiles.forEach { profile ->
            val previous = container.snapshotStore.get(profile.ticker)
            if (previous != null && System.currentTimeMillis() - previous.checkedAtEpochMs < FIVE_MINUTES_MS) return@forEach
            try {
                val dailyNeeded = profile.rules.any { it.timeframe == Timeframe.DAILY }
                val weeklyNeeded = profile.rules.any { it.timeframe == Timeframe.WEEKLY }
                val startDate = LocalDate.now().minusYears(10).toString()
                val daily = if (dailyNeeded) container.marketDataProvider.loadDailyBars(profile.ticker, token, startDate) else emptyList()
                val weekly = if (weeklyNeeded) container.marketDataProvider.loadWeeklyBars(profile.ticker, token, startDate) else emptyList()
                val values = profile.rules.map { rule ->
                    val bars = if (rule.timeframe == Timeframe.DAILY) daily else weekly
                    IndicatorSnapshot(
                        ruleId = rule.id,
                        value = IndicatorEngine.calculate(rule, bars),
                        marketDate = bars.maxByOrNull { it.date }?.date,
                        checkedAtEpochMs = System.currentTimeMillis()
                    )
                }
                container.snapshotStore.save(
                    TickerSnapshot(
                        ticker = profile.ticker,
                        indicators = values,
                        marketDate = values.mapNotNull { it.marketDate }.maxOrNull(),
                        checkedAtEpochMs = System.currentTimeMillis()
                    )
                )
            } catch (error: IOException) {
                result = SyncResult.TransientFailure
            } catch (error: Exception) {
                result = SyncResult.PermanentFailure(error.message ?: "Sync failed")
            }
        }
        if (result == SyncResult.Success) {
            BuyZoneWidget().updateAll(context)
        }
        result
    }

    companion object {
        const val FIVE_MINUTES_MS = 5 * 60 * 1000L
    }
}

sealed interface SyncResult {
    data object Success : SyncResult
    data object NothingToDo : SyncResult
    data object MissingToken : SyncResult
    data object TransientFailure : SyncResult
    data class PermanentFailure(val message: String) : SyncResult
}
