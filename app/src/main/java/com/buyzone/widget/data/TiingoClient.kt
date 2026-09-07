package com.buyzone.widget.data

import com.buyzone.widget.domain.PriceBar
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

interface MarketDataProvider {
    suspend fun loadDailyBars(ticker: String, token: String, startDate: String): List<PriceBar>
    suspend fun loadWeeklyBars(ticker: String, token: String, startDate: String): List<PriceBar>
}

class TiingoClient(private val http: OkHttpClient = OkHttpClient()) : MarketDataProvider {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadDailyBars(ticker: String, token: String, startDate: String): List<PriceBar> =
        request(ticker, token, startDate, "daily")

    override suspend fun loadWeeklyBars(ticker: String, token: String, startDate: String): List<PriceBar> =
        request(ticker, token, startDate, "weekly")

    private fun request(ticker: String, token: String, startDate: String, frequency: String): List<PriceBar> {
        val url = "https://api.tiingo.com/tiingo/daily/${ticker.uppercase()}/prices?startDate=$startDate&resampleFreq=$frequency"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Token $token")
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Tiingo request failed: HTTP ${response.code}")
            val body = response.body?.string() ?: error("Tiingo returned an empty response")
            return json.decodeFromString<List<TiingoBar>>(body).map { PriceBar(it.date.substring(0, 10), it.close) }
        }
    }
}

@Serializable
private data class TiingoBar(val date: String, val close: Double)
