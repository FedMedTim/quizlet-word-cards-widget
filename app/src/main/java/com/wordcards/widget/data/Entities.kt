package com.wordcards.widget.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sets")
data class SetEntity(
    @PrimaryKey val id: String,
    val title: String,
    val totalTerms: Int,
    val lastSyncAt: Long
)

@Entity(
    tableName = "terms",
    indices = [Index(value = ["setId", "rank"])]
)
data class TermEntity(
    @PrimaryKey val id: Long,
    val setId: String,
    val rank: Int,
    val word: String,
    val definition: String,
    /**
     * Quizlet не отдаёт транскрипцию отдельным полем. Если в определении есть
     * фрагмент в косых чертах (/ˈwɜːd/), он вынимается сюда — на 4×2 он идёт
     * под словом отдельной строкой, как в макете.
     */
    val transcription: String?,
    val wordTtsUrl: String?,
    val definitionTtsUrl: String?,
    val wordLang: String?,
    val definitionLang: String?,
    /** Отметка «выучено». Локальная, см. ProgressSource. */
    val learned: Boolean = false
)
