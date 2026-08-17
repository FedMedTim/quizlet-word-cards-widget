package com.wordcards.widget.widget

import androidx.glance.unit.ColorProvider
import com.wordcards.widget.R

/**
 * Палитра из макета.
 *
 * Цвета берутся ссылками на ресурсы, а не литералами: Glance в этой версии не
 * умеет принимать пару день/ночь, зато ресурс сам подставляет вариант из
 * values-night. Заодно те же имена используют drawable карточки, так что
 * палитра описана в одном месте.
 */
object WidgetTheme {
    val accent = ColorProvider(R.color.accent)
    val ink = ColorProvider(R.color.ink)
    val onAccent = ColorProvider(R.color.on_accent)
    val muted = ColorProvider(R.color.muted)
    val track = ColorProvider(R.color.track)

    /** Текст на красной заливке перевёрнутой карточки — белый в обеих темах. */
    val onFlipped = ColorProvider(R.color.on_accent)
    val onFlippedMuted = ColorProvider(R.color.on_flipped_muted)
}
