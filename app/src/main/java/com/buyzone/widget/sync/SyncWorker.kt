package com.buyzone.widget.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // The coordinator will load profiles, enforce the five-minute guard, fetch required
        // timeframes, calculate snapshots, and invalidate all widgets bound to each ticker.
        return Result.success()
    }

    companion object {
        const val TICKER_KEY = "ticker"
    }
}
