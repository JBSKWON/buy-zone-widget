package com.buyzone.widget.data

import android.content.Context
import com.buyzone.widget.domain.IndicatorSnapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TickerSnapshot(
    val ticker: String,
    val indicators: List<IndicatorSnapshot>,
    val marketDate: String?,
    val checkedAtEpochMs: Long,
    val error: String? = null
)

class SnapshotStore(context: Context) {
    private val preferences = context.getSharedPreferences("indicator_snapshots", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun get(ticker: String): TickerSnapshot? = preferences.getString(key(ticker), null)?.let {
        runCatching { json.decodeFromString<TickerSnapshot>(it) }.getOrNull()
    }

    fun save(snapshot: TickerSnapshot) {
        preferences.edit().putString(key(snapshot.ticker), json.encodeToString(snapshot)).apply()
    }

    private fun key(ticker: String) = "snapshot_${ticker.uppercase()}"
}
