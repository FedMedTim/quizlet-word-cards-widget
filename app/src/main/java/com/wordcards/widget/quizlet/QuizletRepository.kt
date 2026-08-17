package com.wordcards.widget.quizlet

import android.content.Context
import com.wordcards.widget.data.AppDatabase
import com.wordcards.widget.data.SetEntity
import com.wordcards.widget.data.Settings
import com.wordcards.widget.data.TermEntity

class QuizletRepository(context: Context) {

    private val dao = AppDatabase.get(context).wordDao()
    private val settings = Settings(context)
    private val client = QuizletWebClient(context)

    fun hasSession(): Boolean = client.hasSessionCookies()

    fun signOut() = client.clearSession()

    /**
     * Тянет набор целиком и кладёт в локальную базу. Виджет всегда рисует из
     * базы, поэтому неудачная синхронизация не оставляет пустой экран —
     * показывается прошлое содержимое и пометка, что синхронизация отстала.
     */
    suspend fun syncSet(setId: String): QuizletResult<SetEntity> {
        if (!client.hasSessionCookies()) {
            return QuizletResult.Error(Reason.FORBIDDEN, "Нужен вход в Quizlet")
        }

        val termsResult = client.fetchTerms(setId)
        if (termsResult is QuizletResult.Error) {
            settings.lastSyncFailed = true
            return termsResult
        }
        val terms = (termsResult as QuizletResult.Ok).value

        // Название набора — не критично: без него берём прошлое или заглушку.
        val meta = client.fetchSetMeta(setId).valueOrNull()
        val previous = dao.getSet(setId)
        val title = meta?.title?.takeIf { it.isNotBlank() }
            ?: previous?.title
            ?: "Набор $setId"

        val now = System.currentTimeMillis()
        val set = SetEntity(
            id = setId,
            title = title,
            totalTerms = terms.size,
            lastSyncAt = now
        )

        dao.replaceSetContents(
            set = set,
            terms = terms.mapIndexed { index, term ->
                TermEntity(
                    id = term.id,
                    setId = setId,
                    rank = if (term.rank == Int.MAX_VALUE) index else term.rank,
                    word = term.word,
                    definition = term.definition,
                    transcription = term.transcription,
                    wordTtsUrl = term.wordTtsUrl,
                    definitionTtsUrl = term.definitionTtsUrl,
                    wordLang = term.wordLang,
                    definitionLang = term.definitionLang
                )
            }
        )

        settings.lastSyncAt = now
        settings.lastSyncFailed = false
        return QuizletResult.Ok(set)
    }
}
