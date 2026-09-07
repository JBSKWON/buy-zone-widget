package com.buyzone.widget.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
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
import com.buyzone.widget.sync.WorkScheduler

class BuyZoneWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BuyZoneWidget()
}

class BuyZoneWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val ticker = context.getSharedPreferences(BINDINGS, Context.MODE_PRIVATE)
            .getString("widget_$appWidgetId", null)
        provideContent {
            Column(modifier = GlanceModifier.padding(16.dp)) {
                Text("Buy Zone Widget")
                Text(ticker ?: "Configure a ticker")
                if (ticker != null) {
                    Text("↻ Refresh", modifier = GlanceModifier.clickable(
                        actionRunCallback<RefreshTickerAction>(
                            actionParametersOf(TickerParameters.TICKER to ticker)
                        )
                    ))
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
