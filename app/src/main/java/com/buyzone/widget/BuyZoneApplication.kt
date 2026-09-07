package com.buyzone.widget

import android.app.Application
import com.buyzone.widget.data.AppContainer
import com.buyzone.widget.sync.WorkScheduler

class BuyZoneApplication : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        WorkScheduler.schedulePeriodic(this)
    }
}
