package com.wordcards.widget.widget

import androidx.compose.ui.graphics.Color
// Пара день/ночь живёт в appwidget-варианте ColorProvider; тот, что в
// androidx.glance.unit, принимает только один цвет.
import androidx.glance.appwidget.unit.ColorProvider

/**
 * Палитра из макета. Заданы явными парами день/ночь, а не ссылками на ресурсы:
 * Glance подставляет нужный вариант сам, и цвет виден прямо в коде разметки.
 */
object WidgetTheme {
    val accent = ColorProvider(day = Color(0xFFE8321E), night = Color(0xFFF2452F))
    val ink = ColorProvider(day = Color(0xFF111111), night = Color(0xFFF5F3F0))
    val onAccent = ColorProvider(day = Color.White, night = Color.White)
    val muted = ColorProvider(day = Color(0xFF6B6B6B), night = Color(0xFF9A9AA0))
    val track = ColorProvider(day = Color(0xFFD8D5D0), night = Color(0xFF33333A))

    /** Текст на красной заливке перевёрнутой карточки. */
    val onFlipped = ColorProvider(day = Color.White, night = Color.White)
    val onFlippedMuted = ColorProvider(day = Color(0xCCFFFFFF), night = Color(0xCCFFFFFF))
}
