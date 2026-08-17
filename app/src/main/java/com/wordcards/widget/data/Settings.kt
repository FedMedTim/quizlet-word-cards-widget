package com.wordcards.widget.data

import android.content.Context
import java.time.LocalDate

/**
 * Настройки живут в SharedPreferences, а не в DataStore, потому что их читает
 * рендер виджета — там нужен дешёвый синхронный доступ без запуска корутины.
 *
 * Часть ключей глобальная (набор по умолчанию, стрик), часть привязана к
 * конкретному экземпляру виджета: на экране их может быть несколько, и каждый
 * листается независимо.
 */
class Settings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("wordcards", Context.MODE_PRIVATE)

    // --- глобальное ---

    var defaultSetId: String?
        get() = prefs.getString(KEY_DEFAULT_SET, null)
        set(value) = prefs.edit().putString(KEY_DEFAULT_SET, value).apply()

    var lastSyncAt: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

    var lastSyncFailed: Boolean
        get() = prefs.getBoolean(KEY_SYNC_FAILED, false)
        set(value) = prefs.edit().putBoolean(KEY_SYNC_FAILED, value).apply()

    var showAudioButton: Boolean
        get() = prefs.getBoolean(KEY_SHOW_AUDIO, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_AUDIO, value).apply()

    var streakDays: Int
        get() = prefs.getInt(KEY_STREAK, 0)
        private set(value) = prefs.edit().putInt(KEY_STREAK, value).apply()

    /**
     * Стрик считается по дням, в которые виджет трогали. Вызывается на каждом
     * взаимодействии; за один день засчитывается один раз.
     */
    fun touchStreak(today: LocalDate = LocalDate.now()) {
        val lastDay = prefs.getLong(KEY_STREAK_DAY, 0L)
        val todayEpoch = today.toEpochDay()
        if (lastDay == todayEpoch) return
        streakDays = when (lastDay) {
            todayEpoch - 1 -> streakDays + 1
            else -> 1
        }
        prefs.edit().putLong(KEY_STREAK_DAY, todayEpoch).apply()
    }

    // --- на экземпляр виджета ---

    fun setIdFor(widgetId: Int): String? =
        prefs.getString(keyWidgetSet(widgetId), null) ?: defaultSetId

    fun bindWidget(widgetId: Int, setId: String) {
        prefs.edit()
            .putString(keyWidgetSet(widgetId), setId)
            .putInt(keyWidgetIndex(widgetId), 0)
            .putBoolean(keyWidgetFlipped(widgetId), false)
            .apply()
        if (defaultSetId == null) defaultSetId = setId
    }

    fun indexFor(widgetId: Int): Int = prefs.getInt(keyWidgetIndex(widgetId), 0)

    fun setIndex(widgetId: Int, index: Int) {
        prefs.edit().putInt(keyWidgetIndex(widgetId), index).apply()
    }

    fun isFlipped(widgetId: Int): Boolean = prefs.getBoolean(keyWidgetFlipped(widgetId), false)

    fun setFlipped(widgetId: Int, flipped: Boolean) {
        prefs.edit().putBoolean(keyWidgetFlipped(widgetId), flipped).apply()
    }

    fun forgetWidget(widgetId: Int) {
        prefs.edit()
            .remove(keyWidgetSet(widgetId))
            .remove(keyWidgetIndex(widgetId))
            .remove(keyWidgetFlipped(widgetId))
            .apply()
    }

    private fun keyWidgetSet(id: Int) = "w_${id}_set"
    private fun keyWidgetIndex(id: Int) = "w_${id}_index"
    private fun keyWidgetFlipped(id: Int) = "w_${id}_flipped"

    private companion object {
        const val KEY_DEFAULT_SET = "default_set"
        const val KEY_LAST_SYNC = "last_sync"
        const val KEY_SYNC_FAILED = "sync_failed"
        const val KEY_SHOW_AUDIO = "show_audio"
        const val KEY_STREAK = "streak_days"
        const val KEY_STREAK_DAY = "streak_day"
    }
}
