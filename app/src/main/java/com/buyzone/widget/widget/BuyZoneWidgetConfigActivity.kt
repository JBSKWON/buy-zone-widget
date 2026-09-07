package com.buyzone.widget.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.buyzone.widget.BuyZoneApplication
import com.buyzone.widget.domain.TickerProfile

class BuyZoneWidgetConfigActivity : ComponentActivity() {
    private var ticker by mutableStateOf("")
    private var widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        setResult(Activity.RESULT_CANCELED)
        setContent {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Choose a ticker")
                OutlinedTextField(value = ticker, onValueChange = { ticker = it.uppercase() }, label = { Text("Ticker") })
                Button(onClick = ::save) { Text("Add widget") }
            }
        }
    }

    private fun save() {
        val normalized = ticker.trim().uppercase()
        if (normalized.isBlank() || widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        getSharedPreferences("widget_bindings", MODE_PRIVATE)
            .edit().putString("widget_$widgetId", normalized).apply()
        (application as BuyZoneApplication).container.profileStore.get(normalized)
            ?: (application as BuyZoneApplication).container.profileStore.save(TickerProfile(normalized))
        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
}
