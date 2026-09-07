package com.buyzone.widget.data

import android.content.Context
import okhttp3.OkHttpClient

class AppContainer(context: Context) {
    val marketDataProvider: MarketDataProvider = TiingoClient(OkHttpClient())
    val preferences = context.getSharedPreferences("buy_zone", Context.MODE_PRIVATE)
    val tokenStore = TokenStore(context)
    val profileStore = ProfileStore(context)
    val snapshotStore = SnapshotStore(context)
}
