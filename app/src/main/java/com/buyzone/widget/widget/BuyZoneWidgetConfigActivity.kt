package com.buyzone.widget.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.buyzone.widget.MainActivity

/**
 * Android requires a configuration activity for a widget that needs a binding.
 * The actual ticker and strategy configuration lives in MainActivity.
 */
class BuyZoneWidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_WIDGET_ID, widgetId))
        finish()
    }
}
