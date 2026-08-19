package com.wordcards.widget.widget

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import com.wordcards.widget.WordCardsApp
import com.wordcards.widget.data.AppDatabase
import com.wordcards.widget.data.Settings
import kotlinx.coroutines.delay
import java.util.Locale

private suspend fun widgetIdOf(context: Context, glanceId: GlanceId): Int =
    GlanceAppWidgetManager(context).getAppWidgetId(glanceId)

/**
 * Тап по телу карточки: показать перевод и обратно.
 *
 * Переворот засчитывается как «карточку посмотрели» — именно это двигает
 * полосу прогресса, других сигналов об изучении у виджета нет.
 */
class FlipAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val widgetId = widgetIdOf(context, glanceId)
        val settings = Settings(context)
        val nowFlipped = !settings.isFlipped(widgetId)
        settings.setFlipped(widgetId, nowFlipped)
        settings.touchStreak()

        if (nowFlipped) {
            val setId = settings.setIdFor(widgetId)
            if (setId != null) {
                val dao = AppDatabase.get(context).wordDao()
                dao.getTermAt(setId, settings.indexFor(widgetId))
                    ?.let { dao.setLearned(it.id, true) }
            }
        }

        WidgetUpdater.refreshOne(context, glanceId)
    }
}

/** Стрелка на 4×2 и плашка-заголовок на 2×2: следующее слово того же набора. */
class NextWordAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val widgetId = widgetIdOf(context, glanceId)
        val settings = Settings(context)
        val setId = settings.setIdFor(widgetId) ?: return
        val total = AppDatabase.get(context).wordDao().countTerms(setId)
        if (total == 0) return

        settings.setIndex(widgetId, (settings.indexFor(widgetId) + 1) % total)
        settings.setFlipped(widgetId, false)
        settings.touchStreak()
        WidgetUpdater.refreshOne(context, glanceId)
    }
}

/**
 * Озвучка слова системным TTS.
 *
 * Quizlet отдаёт свои ttsUrl, но они живут за той же защитой, что и API, и
 * проигрыватель получил бы 403. Системный движок работает офлайн и без сессии.
 */
class SpeakAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val app = context.applicationContext as? WordCardsApp ?: return
        val engine = awaitTts(app) ?: return

        val widgetId = widgetIdOf(context, glanceId)
        val settings = Settings(context)
        val setId = settings.setIdFor(widgetId) ?: return
        val term = AppDatabase.get(context).wordDao()
            .getTermAt(setId, settings.indexFor(widgetId)) ?: return

        val flipped = settings.isFlipped(widgetId)
        val text = if (flipped) term.definition else term.word
        val lang = if (flipped) term.definitionLang else term.wordLang

        lang?.let { code ->
            val locale = Locale.forLanguageTag(code)
            if (engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
                engine.language = locale
            }
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "wordcard-$widgetId")
    }
}

/**
 * Ждёт готовности движка озвучки.
 *
 * Инициализация занимает сотни миллисекунд, и на холодном процессе первый тап
 * приходил раньше — звук молча не воспроизводился. Теперь нажатие ждёт движок,
 * а сдаётся только если он так и не поднялся.
 */
private suspend fun awaitTts(app: WordCardsApp): TextToSpeech? {
    repeat(20) {
        if (app.ttsReady) return app.tts
        delay(100)
    }
    return null
}
