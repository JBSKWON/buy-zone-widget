package com.buyzone.widget.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return when (val result = SyncCoordinator(applicationContext).sync(inputData.getString(TICKER_KEY))) {
            SyncResult.Success, SyncResult.NothingToDo, SyncResult.MissingToken,
            is SyncResult.PermanentFailure -> Result.success()
            SyncResult.TransientFailure -> Result.retry()
        }
    }

    companion object {
        const val TICKER_KEY = "ticker"
    }
}
