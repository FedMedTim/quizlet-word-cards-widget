package com.wordcards.widget.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll

object WidgetUpdater {

    /** Полное обновление: после синхронизации, когда поменялись данные наборов. */
    suspend fun refreshAll(context: Context) {
        SmallWidget().updateAll(context)
        WideWidget().updateAll(context)
    }

    /**
     * Обновление одного экземпляра — реакция на тап.
     *
     * Перерисовывать на каждое нажатие все виджеты обоих типов слишком дорого:
     * каждый заново вычитывает набор из базы, и отклик растягивается на секунды.
     * Нажатие меняет состояние только той карточки, по которой нажали.
     */
    suspend fun refreshOne(context: Context, glanceId: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val provider = AppWidgetManager.getInstance(context)
            .getAppWidgetInfo(widgetId)?.provider?.className

        when (provider) {
            SmallWidgetReceiver::class.java.name -> SmallWidget().update(context, glanceId)
            WideWidgetReceiver::class.java.name -> WideWidget().update(context, glanceId)
            // Тип не определился — редкий случай, лучше обновить всё, чем ничего.
            else -> refreshAll(context)
        }
    }
}
