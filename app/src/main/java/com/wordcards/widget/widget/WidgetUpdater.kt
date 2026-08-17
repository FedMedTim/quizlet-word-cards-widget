package com.wordcards.widget.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object WidgetUpdater {
    suspend fun refreshAll(context: Context) {
        SmallWidget().updateAll(context)
        WideWidget().updateAll(context)
    }
}
