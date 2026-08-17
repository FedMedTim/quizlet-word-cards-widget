package com.wordcards.widget.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
// Перегрузка, принимающая Intent, есть только в appwidget-пакете:
// androidx.glance.action знает про ComponentName, но не про Intent.
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordcards.widget.R
import com.wordcards.widget.ui.ConfigActivity

/**
 * Общие детали обеих раскладок. Карточка построена как в макете: чёрная рамка
 * 2dp, цветная плашка-заголовок вплотную к ней, тело со словом и подвал с
 * прогрессом. Инсет в 2dp по краям нужен, чтобы содержимое не наезжало на рамку.
 */

private const val CARD_INSET = 2
private const val BRACKET_INSET = 5

@Composable
fun CardSurface(flipped: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(
                ImageProvider(
                    if (flipped) R.drawable.flipped_surface else R.drawable.card_surface
                )
            )
    ) {
        Column(modifier = GlanceModifier.fillMaxSize().padding(CARD_INSET.dp)) {
            content()
        }
        CornerBrackets(flipped)
    }
}

/**
 * Угловые скобки из макета.
 *
 * Каждая — вектор фиксированного размера в своём слое поверх карточки, поэтому
 * при любом размере виджета они остаются одинаковыми, а не растягиваются.
 * Слои не кликабельны, поэтому тап проваливается сквозь них на тело карточки.
 */
@Composable
private fun CornerBrackets(flipped: Boolean) {
    // Верхняя пара ложится на цветную плашку, нижняя — на тело карточки.
    val top = WidgetTheme.onAccent
    val bottom = if (flipped) WidgetTheme.onFlipped else WidgetTheme.ink

    Bracket(R.drawable.bracket_tl, Alignment.TopStart, top)
    Bracket(R.drawable.bracket_tr, Alignment.TopEnd, top)
    Bracket(R.drawable.bracket_bl, Alignment.BottomStart, bottom)
    Bracket(R.drawable.bracket_br, Alignment.BottomEnd, bottom)
}

@Composable
private fun Bracket(resId: Int, alignment: Alignment, tint: ColorProvider) {
    Box(
        modifier = GlanceModifier.fillMaxSize().padding(BRACKET_INSET.dp),
        contentAlignment = alignment
    ) {
        Image(
            provider = ImageProvider(resId),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = GlanceModifier.size(9.dp)
        )
    }
}

/**
 * Плашка-заголовок. В обычном состоянии красная с названием набора, в
 * перевёрнутом — чёрная с подписью «ПЕРЕВОД», как в макете.
 */
@Composable
fun HeaderStripe(
    title: String,
    flipped: Boolean,
    showAudio: Boolean,
    streak: Int?,
    compact: Boolean,
    onTapNext: GlanceModifier
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(
                ImageProvider(
                    if (flipped) R.drawable.header_stripe_dark else R.drawable.header_stripe
                )
            )
            .padding(horizontal = if (compact) 7.dp else 10.dp, vertical = 4.dp)
            .then(onTapNext),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // На широкой карточке перед названием стоит значок связи с аккаунтом.
        if (!compact && !flipped) {
            Image(
                provider = ImageProvider(R.drawable.ic_link),
                contentDescription = null,
                colorFilter = ColorFilter.tint(WidgetTheme.onAccent),
                modifier = GlanceModifier.size(11.dp)
            )
            Spacer(modifier = GlanceModifier.width(5.dp))
        }

        Text(
            text = if (flipped) "ПЕРЕВОД" else title.uppercase(),
            maxLines = 1,
            style = TextStyle(
                color = WidgetTheme.onAccent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.defaultWeight()
        )

        if (!flipped && showAudio) {
            Image(
                provider = ImageProvider(R.drawable.ic_volume),
                contentDescription = "Озвучить",
                colorFilter = ColorFilter.tint(WidgetTheme.onAccent),
                modifier = GlanceModifier
                    .size(if (compact) 12.dp else 13.dp)
                    .clickable(actionRunCallback<SpeakAction>())
            )
        }

        if (!flipped && streak != null && streak > 0) {
            Spacer(modifier = GlanceModifier.width(5.dp))
            Box(
                modifier = GlanceModifier
                    .background(ImageProvider(R.drawable.streak_badge))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = streak.toString(),
                    style = TextStyle(
                        color = WidgetTheme.onAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun ProgressFooter(
    learned: Int,
    total: Int,
    progress: Float,
    caption: String,
    horizontal: Int
) {
    LinearProgressIndicator(
        progress = progress,
        color = WidgetTheme.accent,
        backgroundColor = WidgetTheme.track,
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(4.dp)
            .padding(horizontal = horizontal.dp)
    )
    Spacer(modifier = GlanceModifier.height(4.dp))
    Text(
        text = caption,
        maxLines = 1,
        style = TextStyle(
            color = WidgetTheme.ink,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        ),
        modifier = GlanceModifier.padding(horizontal = horizontal.dp, vertical = 0.dp)
    )
    Spacer(modifier = GlanceModifier.height(7.dp))
}

@Composable
fun FlippedFooter(horizontal: Int) {
    Text(
        text = "✓ Тап по карточке — вернуть слово",
        maxLines = 1,
        style = TextStyle(
            color = WidgetTheme.onFlippedMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        ),
        modifier = GlanceModifier.padding(horizontal = horizontal.dp)
    )
    Spacer(modifier = GlanceModifier.height(7.dp))
}

/**
 * Экран-заглушка для состояний, когда рисовать карточку нечего: виджет не
 * привязан к набору, сессия истекла или набор пуст. Тап открывает настройку.
 */
@Composable
fun PromptCard(context: Context, widgetId: Int, title: String, hint: String) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.card_surface))
            .clickable(actionStartActivity(configIntent(context, widgetId))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.ic_link),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(WidgetTheme.accent),
                    modifier = GlanceModifier.size(14.dp)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = title,
                    maxLines = 1,
                    style = TextStyle(
                        color = WidgetTheme.accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = hint,
                maxLines = 3,
                style = TextStyle(
                    color = WidgetTheme.ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

fun configIntent(context: Context, widgetId: Int): Intent =
    Intent(context, ConfigActivity::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Разные виджеты должны получать разные PendingIntent, иначе система
        // переиспользует один и настройка открывается не для того экземпляра.
        data = Uri.parse("wordcards://config/$widgetId")
    }

fun labelColor(flipped: Boolean): ColorProvider =
    if (flipped) WidgetTheme.onFlipped else WidgetTheme.ink
