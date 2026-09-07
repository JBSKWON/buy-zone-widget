package com.buyzone.widget.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.compose.ui.unit.dp
import com.buyzone.widget.BuyZoneApplication
import com.buyzone.widget.domain.StrategyEvaluator
import com.buyzone.widget.sync.WorkScheduler

class BuyZoneWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BuyZoneWidget()
}

class BuyZoneWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val ticker = context.getSharedPreferences(BINDINGS, Context.MODE_PRIVATE)
            .getString("widget_$appWidgetId", null)
        val container = (context.applicationContext as BuyZoneApplication).container
        val profile = ticker?.let { container.profileStore.get(it) }
        val snapshot = ticker?.let { container.snapshotStore.get(it) }
        val stage = if (profile != null && snapshot != null) {
            StrategyEvaluator.stage(profile, snapshot.indicators)
        } else null
        provideContent {
            Column(modifier = GlanceModifier.padding(16.dp)) {
                Text("Buy Zone Widget")
                Text(ticker ?: "Configure a ticker")
                if (ticker != null) {
                    Text(if (profile?.rules.isNullOrEmpty()) "Strategy setup required" else "Stage ${stage ?: "—"} / ${profile?.stageCount}")
                    snapshot?.indicators?.take(2)?.forEach { value ->
                        Text("${value.ruleId}: ${value.value?.let { "%.2f".format(it) } ?: "—"}")
                    }
                    Text(snapshot?.marketDate?.let { "Market date: $it" } ?: "No data yet")
                    Text("↻ Refresh", modifier = GlanceModifier.clickable(
                        actionRunCallback<RefreshTickerAction>(
                            actionParametersOf(TickerParameters.TICKER to ticker)
                        )
                    ))
                } else {
                    Text("Configure in app", modifier = GlanceModifier.clickable(actionStartActivity<com.buyzone.widget.MainActivity>()))
                }
            }
        }
    }

    private companion object {
        const val BINDINGS = "widget_bindings"
    }
}

class RefreshTickerAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val ticker = parameters[TickerParameters.TICKER] ?: return
        WorkScheduler.requestTickerRefresh(context, ticker)
    }
}

object TickerParameters {
    val TICKER = ActionParameters.Key<String>("ticker")
}
