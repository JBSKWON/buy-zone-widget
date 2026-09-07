package com.buyzone.widget.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
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
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.dp
import com.buyzone.widget.BuyZoneApplication
import com.buyzone.widget.domain.StageColors
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
        val stageNumber = stage ?: 0
        val stageCount = profile?.stageCount ?: 3
        val accent = colorProvider(StageColors.color(stageNumber, stageCount))
        val surface = colorProvider(StageColors.backgroundColor(stageNumber, stageCount))
        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxWidth()
                    .background(surface)
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Text(
                    "BUY ZONE / ${ticker ?: "SETUP"}",
                    style = TextStyle(
                        color = colorProvider(Color(0xFFB8BEC9)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.height(6.dp))
                if (ticker != null) {
                    Text(
                        stageLabel(stage, profile?.stageCount ?: 3),
                        style = TextStyle(
                            color = accent,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        if (profile?.rules.isNullOrEmpty()) "Strategy setup required" else "Stage ${stage ?: "—"} / ${profile?.stageCount}",
                        style = TextStyle(
                            color = colorProvider(Color(0xFFE7E9ED)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(GlanceModifier.height(10.dp))
                    snapshot?.indicators?.take(3)?.forEach { value ->
                        Row(modifier = GlanceModifier.fillMaxWidth()) {
                            Text(
                                value.ruleId.replace('-', ' ').uppercase(),
                                modifier = GlanceModifier.defaultWeight(),
                                style = TextStyle(color = colorProvider(Color(0xFF9AA1AE)), fontSize = 12.sp)
                            )
                            Text(
                                value.value?.let { "%.2f".format(it) } ?: "—",
                                style = TextStyle(color = colorProvider(Color(0xFFF2F3F5)), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                    Spacer(GlanceModifier.height(8.dp))
                    Text(
                        snapshot?.marketDate?.let { "Market close · $it" } ?: "Waiting for first market update",
                        style = TextStyle(color = colorProvider(Color(0xFF9AA1AE)), fontSize = 11.sp)
                    )
                    Spacer(GlanceModifier.height(6.dp))
                    Text(
                        "↻  Refresh",
                        modifier = GlanceModifier.clickable(
                            actionRunCallback<RefreshTickerAction>(
                                actionParametersOf(TickerParameters.TICKER to ticker)
                            )
                        ),
                        style = TextStyle(color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    )
                } else {
                    Text(
                        "Configure in app",
                        modifier = GlanceModifier.clickable(actionStartActivity<com.buyzone.widget.MainActivity>()),
                        style = TextStyle(color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    private fun stageLabel(stage: Int?, stageCount: Int): String = when {
        stage == null -> "NO SIGNAL"
        stage <= 0 -> "WAIT"
        stageCount == 3 && stage == 1 -> "BUY · START"
        stageCount == 3 && stage == 2 -> "BUY · ATTRACTIVE"
        stageCount == 3 && stage == 3 -> "BUY · MUST BUY"
        else -> "BUY · STAGE $stage"
    }

    private fun colorProvider(argb: Int): ColorProvider = ColorProvider(
        Color(
            red = (argb ushr 16 and 0xFF) / 255f,
            green = (argb ushr 8 and 0xFF) / 255f,
            blue = (argb and 0xFF) / 255f,
            alpha = (argb ushr 24 and 0xFF) / 255f
        )
    )

    private fun colorProvider(color: Color): ColorProvider = ColorProvider(color)

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
