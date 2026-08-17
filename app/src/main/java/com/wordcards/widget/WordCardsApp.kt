package com.wordcards.widget

import android.app.Application
import android.speech.tts.TextToSpeech
import com.wordcards.widget.data.AppDatabase
import com.wordcards.widget.data.Settings
import com.wordcards.widget.sync.SyncScheduler

class WordCardsApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val settings: Settings by lazy { Settings(this) }

    /**
     * TTS живёт на уровне процесса: инициализация движка занимает сотни миллисекунд,
     * а тап по иконке звука должен отзываться сразу.
     */
    @Volatile
    var tts: TextToSpeech? = null
        private set

    @Volatile
    var ttsReady: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
        SyncScheduler.ensureScheduled(this)
    }

    companion object {
        @Volatile
        lateinit var instance: WordCardsApp
            private set
    }
}
