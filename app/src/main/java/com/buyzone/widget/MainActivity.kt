package com.buyzone.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import com.buyzone.widget.data.ProfileStore
import com.buyzone.widget.data.TokenStore
import com.buyzone.widget.domain.CombinationMode
import com.buyzone.widget.domain.IndicatorRule
import com.buyzone.widget.domain.IndicatorType
import com.buyzone.widget.domain.ProfileEditor
import com.buyzone.widget.domain.TickerProfile
import com.buyzone.widget.widget.BuyZoneWidget
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        setContent {
            ConfigurationScreen(
                tokenStore = TokenStore(this),
                profileStore = (application as BuyZoneApplication).container.profileStore,
                pendingWidgetId = widgetId,
                onWidgetBound = { ticker ->
                    getSharedPreferences(WIDGET_BINDINGS, MODE_PRIVATE)
                        .edit().putString("widget_$widgetId", ticker).apply()
                    setResult(Activity.RESULT_OK)
                    lifecycleScope.launch { BuyZoneWidget().updateAll(this@MainActivity) }
                }
            )
        }
    }

    companion object {
        const val EXTRA_WIDGET_ID = "com.buyzone.widget.EXTRA_WIDGET_ID"
        private const val WIDGET_BINDINGS = "widget_bindings"
    }
}

@Composable
private fun ConfigurationScreen(
    tokenStore: TokenStore,
    profileStore: ProfileStore,
    pendingWidgetId: Int,
    onWidgetBound: (String) -> Unit
) {
    var token by remember { mutableStateOf(tokenStore.read().orEmpty()) }
    var tickerInput by remember { mutableStateOf("") }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var profiles by remember { mutableStateOf(profileStore.all().sortedBy { it.ticker }) }

    fun reload() { profiles = profileStore.all().sortedBy { it.ticker } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Buy Zone Widget", style = MaterialTheme.typography.headlineMedium)
            Text("Configure tickers and strategies here. Widgets only display and refresh data.")
        }
        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = token,
                onValueChange = { token = it; savedMessage = null },
                label = { Text("Tiingo API token") }
            )
            Button(onClick = { tokenStore.save(token); savedMessage = "Token saved securely." }) {
                Text("Save token")
            }
            savedMessage?.let { Text(it) }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text("Tickers", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = tickerInput,
                    onValueChange = { tickerInput = it.uppercase() },
                    label = { Text("Add ticker") }
                )
                Button(onClick = {
                    val ticker = tickerInput.trim()
                    if (ticker.isNotBlank() && profileStore.get(ticker) == null) {
                        profileStore.save(TickerProfile(ticker))
                        tickerInput = ""
                        reload()
                    }
                }) { Text("Add") }
            }
            if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                Text("Choose which ticker this widget should display.")
            }
        }
        items(profiles, key = { it.ticker }) { profile ->
            ProfileCard(
                profile = profile,
                profileStore = profileStore,
                pendingWidgetId = pendingWidgetId,
                onChanged = ::reload,
                onWidgetBound = onWidgetBound
            )
        }
    }
}

@Composable
private fun ProfileCard(
    profile: TickerProfile,
    profileStore: ProfileStore,
    pendingWidgetId: Int,
    onChanged: () -> Unit,
    onWidgetBound: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(profile.ticker, style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Stages: ${profile.stageCount}")
            TextButton(onClick = {
                profileStore.save(ProfileEditor.setStageCount(profile, profile.stageCount - 1))
                onChanged()
            }) { Text("−") }
            TextButton(onClick = {
                profileStore.save(ProfileEditor.setStageCount(profile, profile.stageCount + 1))
                onChanged()
            }) { Text("+") }
            TextButton(onClick = {
                profileStore.save(profile.copy(combinationMode = if (profile.combinationMode == CombinationMode.OR) CombinationMode.AND else CombinationMode.OR))
                onChanged()
            }) { Text("Mode: ${profile.combinationMode}") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                profileStore.save(ProfileEditor.addIndicator(profile, IndicatorType.RSI))
                onChanged()
            }) { Text("Add weekly RSI") }
            Button(onClick = {
                profileStore.save(ProfileEditor.addIndicator(profile, IndicatorType.SMA_DEVIATION))
                onChanged()
            }) { Text("Add daily SMA deviation") }
        }
        if (profile.rules.isEmpty()) Text("No indicators configured")
        profile.rules.forEach { rule ->
            RuleEditor(profile, rule, profileStore, onChanged)
        }
        if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            Button(onClick = { onWidgetBound(profile.ticker) }) { Text("Use ${profile.ticker} for this widget") }
        }
    }
}

@Composable
private fun RuleEditor(
    profile: TickerProfile,
    rule: IndicatorRule,
    profileStore: ProfileStore,
    onChanged: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${rule.type} · ${rule.timeframe} · ${rule.comparison}")
            TextButton(onClick = {
                profileStore.save(ProfileEditor.removeIndicator(profile, rule.id))
                onChanged()
            }) { Text("Remove") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            rule.thresholds.forEachIndexed { index, threshold ->
                var text by remember(rule.id, index, threshold) { mutableStateOf(threshold.toString()) }
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = text,
                    onValueChange = { value ->
                        text = value
                        value.toDoubleOrNull()?.let { parsed ->
                            val updated = rule.thresholds.toMutableList().also { it[index] = parsed }
                            profileStore.save(ProfileEditor.updateRule(profile, rule.copy(thresholds = updated)))
                            onChanged()
                        }
                    },
                    label = { Text("Stage ${index + 1}") }
                )
            }
        }
    }
}
