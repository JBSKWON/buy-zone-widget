package com.buyzone.widget.data

import android.content.Context
import com.buyzone.widget.domain.TickerProfile
import kotlinx.serialization.json.Json

class ProfileStore(context: Context) {
    private val preferences = context.getSharedPreferences("ticker_profiles", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun get(ticker: String): TickerProfile? = preferences.getString(key(ticker), null)?.let {
        runCatching { json.decodeFromString<TickerProfile>(it) }.getOrNull()
    }

    fun save(profile: TickerProfile) {
        preferences.edit().putString(key(profile.ticker), json.encodeToString(profile)).apply()
    }

    fun all(): List<TickerProfile> = preferences.all.keys.mapNotNull { key ->
        preferences.getString(key, null)?.let { runCatching { json.decodeFromString<TickerProfile>(it) }.getOrNull() }
    }

    private fun key(ticker: String) = "profile_${ticker.uppercase()}"
}
