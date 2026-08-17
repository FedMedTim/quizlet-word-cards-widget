package com.wordcards.widget.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle

/**
 * Открывается лаунчером при добавлении виджета на экран.
 *
 * Пока не вернём RESULT_OK, система считает добавление отменённым и убирает
 * виджет — поэтому результат выставляется отменённым сразу, а успешным только
 * после того, как набор реально загрузился.
 */
class ConfigActivity : BaseSetupActivity() {

    override val targetWidgetId: Int by lazy {
        intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setResult(Activity.RESULT_CANCELED, resultIntent())
        super.onCreate(savedInstanceState)
    }

    override fun onSetConnected(setId: String) {
        setResult(Activity.RESULT_OK, resultIntent())
        finish()
    }

    private fun resultIntent() = Intent().putExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        targetWidgetId
    )
}
