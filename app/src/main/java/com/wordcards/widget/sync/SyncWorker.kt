package com.wordcards.widget.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wordcards.widget.data.AppDatabase
import com.wordcards.widget.data.Settings
import com.wordcards.widget.quizlet.QuizletRepository
import com.wordcards.widget.quizlet.QuizletResult
import com.wordcards.widget.quizlet.Reason
import com.wordcards.widget.widget.WidgetUpdater

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = Settings(applicationContext)
        val dao = AppDatabase.get(applicationContext).wordDao()
        val repository = QuizletRepository(applicationContext)

        // Синхронизируем все наборы, которые где-то показаны, а не только текущий:
        // виджетов на экране может быть несколько и с разными наборами.
        val setIds = buildSet {
            settings.defaultSetId?.let { add(it) }
            addAll(dao.getAllSets().map { it.id })
        }
        if (setIds.isEmpty()) return Result.success()

        var sawRetryable = false
        for (setId in setIds) {
            when (val result = repository.syncSet(setId)) {
                is QuizletResult.Ok -> Unit
                is QuizletResult.Error -> when (result.reason) {
                    // Сессия истекла — повтор не поможет, нужен вход руками.
                    Reason.FORBIDDEN, Reason.NOT_FOUND -> Unit
                    else -> sawRetryable = true
                }
            }
        }

        WidgetUpdater.refreshAll(applicationContext)
        return if (sawRetryable) Result.retry() else Result.success()
    }

    companion object {
        const val NAME = "quizlet-sync"
    }
}
