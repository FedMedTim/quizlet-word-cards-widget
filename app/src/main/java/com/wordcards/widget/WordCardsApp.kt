package com.wordcards.widget

import android.app.Application
import android.speech.tts.TextToSpeech
import com.wordcards.widget.sync.SyncScheduler

class WordCardsApp : Application() {

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
        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
        SyncScheduler.ensureScheduled(this)
    }
}
