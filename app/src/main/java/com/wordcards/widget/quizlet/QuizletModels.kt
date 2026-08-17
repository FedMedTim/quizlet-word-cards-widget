package com.wordcards.widget.quizlet

data class QuizletTerm(
    val id: Long,
    val rank: Int,
    val word: String,
    val definition: String,
    val transcription: String?,
    val wordTtsUrl: String?,
    val definitionTtsUrl: String?,
    val wordLang: String?,
    val definitionLang: String?
)

data class QuizletSetMeta(
    val id: String,
    val title: String,
    val totalTerms: Int
)

enum class Reason {
    /** 401/403 — сессия истекла или сработала защита от ботов. */
    FORBIDDEN,
    NOT_FOUND,
    TIMEOUT,
    /** Ответ пришёл, но карточек в нём нет — обычно смена формата ответа. */
    EMPTY,
    UNKNOWN
}

sealed interface QuizletResult<out T> {
    data class Ok<T>(val value: T) : QuizletResult<T>
    data class Error(val reason: Reason, val message: String) : QuizletResult<Nothing>
}

inline fun <T, R> QuizletResult<T>.map(transform: (T) -> QuizletResult<R>): QuizletResult<R> =
    when (this) {
        is QuizletResult.Ok -> transform(value)
        is QuizletResult.Error -> this
    }

fun <T> QuizletResult<T>.valueOrNull(): T? = (this as? QuizletResult.Ok)?.value
