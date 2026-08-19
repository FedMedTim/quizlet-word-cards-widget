package com.wordcards.widget.widget

import android.content.Context
import com.wordcards.widget.data.AppDatabase
import com.wordcards.widget.data.Settings
import com.wordcards.widget.quizlet.QuizletWebClient
import java.util.concurrent.TimeUnit

/**
 * Всё, что нужно отрисовать одному экземпляру виджета. Собирается один раз
 * перед композицией, чтобы разметка не ходила в базу.
 */
sealed interface CardState {

    /** Виджет ещё не привязан к набору — предлагаем открыть настройку. */
    data object NeedsSetup : CardState

    /** Сессия Quizlet протухла: данные есть, но обновляться перестали. */
    data class NeedsLogin(val setTitle: String) : CardState

    data class Empty(val setTitle: String) : CardState

    data class Card(
        val setTitle: String,
        val word: String,
        val definition: String,
        val transcription: String?,
        val flipped: Boolean,
        val learned: Int,
        val total: Int,
        val streak: Int,
        val showAudio: Boolean,
        val syncLabel: String
    ) : CardState {
        val progress: Float get() = if (total <= 0) 0f else learned.toFloat() / total
    }

    companion object {

        suspend fun load(context: Context, widgetId: Int): CardState {
            val settings = Settings(context)
            val dao = AppDatabase.get(context).wordDao()

            val setId = settings.setIdFor(widgetId) ?: return NeedsSetup
            val set = dao.getSet(setId) ?: return NeedsSetup
            val total = dao.countTerms(setId)

            if (total == 0) {
                // Проверка кук поднимает подсистему WebView и стоит дорого,
                // поэтому выполняется только когда рисовать всё равно нечего.
                val hasSession = QuizletWebClient(context).hasSessionCookies()
                return if (hasSession) Empty(set.title) else NeedsLogin(set.title)
            }

            // Индекс мог уехать за пределы набора, если карточки удалили в Quizlet.
            val index = settings.indexFor(widgetId).let { if (it in 0 until total) it else 0 }
            val term = dao.getTermAt(setId, index) ?: return Empty(set.title)

            return Card(
                setTitle = set.title,
                word = term.word,
                definition = term.definition.ifBlank { "—" },
                transcription = term.transcription,
                flipped = settings.isFlipped(widgetId),
                learned = dao.countLearned(setId),
                total = total,
                streak = settings.streakDays,
                showAudio = settings.showAudioButton,
                syncLabel = syncLabel(set.lastSyncAt, settings.lastSyncFailed)
            )
        }

        private fun syncLabel(lastSyncAt: Long, failed: Boolean): String {
            if (lastSyncAt == 0L) return "ещё не синхронизировано"
            val minutes = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - lastSyncAt)
            val ago = when {
                minutes < 2 -> "синхронизировано сейчас"
                minutes < 60 -> "синхронизировано $minutes мин назад"
                minutes < 60 * 24 -> "синхронизировано ${minutes / 60} ч назад"
                else -> "синхронизировано ${minutes / (60 * 24)} дн назад"
            }
            return if (failed) "$ago · обновление не прошло" else ago
        }
    }
}
