package com.wordcards.widget.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.wordcards.widget.R

/**
 * Виджет 4×2: слово, транскрипция, кнопка «дальше» и строка синхронизации.
 */
class WideWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val state = CardState.load(context, widgetId)
        provideContent { Content(context, widgetId, state) }
    }

    @Composable
    private fun Content(context: Context, widgetId: Int, state: CardState) {
        when (state) {
            is CardState.NeedsSetup ->
                PromptCard(context, widgetId, "QUIZLET", "Выберите набор карточек")

            is CardState.NeedsLogin ->
                PromptCard(context, widgetId, "QUIZLET", "Войдите в аккаунт Quizlet")

            is CardState.Empty ->
                PromptCard(context, widgetId, state.setTitle, "В наборе нет карточек")

            is CardState.Card -> Card(state)
        }
    }

    @Composable
    private fun Card(state: CardState.Card) {
        CardSurface(state.flipped) {
            HeaderStripe(
                title = "${state.setTitle} · QUIZLET",
                flipped = state.flipped,
                showAudio = state.showAudio,
                streak = null,
                compact = false,
                onTapNext = GlanceModifier
            )

            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(actionRunCallback<FlipAction>())
                ) {
                    Text(
                        text = if (state.flipped) state.definition else state.word,
                        maxLines = 2,
                        style = TextStyle(
                            color = labelColor(state.flipped),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (!state.flipped && !state.transcription.isNullOrBlank()) {
                        Text(
                            text = state.transcription,
                            maxLines = 1,
                            style = TextStyle(
                                color = WidgetTheme.muted,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                Box(
                    modifier = GlanceModifier
                        .size(34.dp)
                        .background(ImageProvider(R.drawable.arrow_button))
                        .clickable(actionRunCallback<NextWordAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_chevron_right),
                        contentDescription = "Следующее слово",
                        colorFilter = ColorFilter.tint(WidgetTheme.onAccent),
                        modifier = GlanceModifier.size(18.dp)
                    )
                }
            }

            if (state.flipped) {
                FlippedFooter(horizontal = 12)
            } else {
                ProgressFooter(
                    learned = state.learned,
                    total = state.total,
                    progress = state.progress,
                    caption = "${state.learned} из ${state.total} слов · ${state.syncLabel}",
                    horizontal = 12
                )
            }
        }
    }
}

class WideWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WideWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val settings = com.wordcards.widget.data.Settings(context)
        appWidgetIds.forEach(settings::forgetWidget)
    }
}
