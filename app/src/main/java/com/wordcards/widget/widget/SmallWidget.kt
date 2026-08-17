package com.wordcards.widget.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

/**
 * Виджет 2×2: только слово и прогресс.
 *
 * Стрелки «дальше» в макете здесь нет — места нет. Поэтому листание повешено на
 * плашку-заголовок, а тело карточки отвечает за переворот.
 */
class SmallWidget : GlanceAppWidget() {

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
                PromptCard(context, widgetId, "QUIZLET", "Войдите в аккаунт")

            is CardState.Empty ->
                PromptCard(context, widgetId, state.setTitle, "В наборе нет карточек")

            is CardState.Card -> Card(state)
        }
    }

    @Composable
    private fun Card(state: CardState.Card) {
        CardSurface(state.flipped) {
            HeaderStripe(
                title = state.setTitle,
                flipped = state.flipped,
                showAudio = state.showAudio,
                streak = state.streak,
                compact = true,
                onTapNext = GlanceModifier.clickable(actionRunCallback<NextWordAction>())
            )

            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .padding(horizontal = 9.dp, vertical = 6.dp)
                    .clickable(actionRunCallback<FlipAction>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.flipped) state.definition else state.word,
                    maxLines = 3,
                    style = TextStyle(
                        color = labelColor(state.flipped),
                        fontSize = if (state.flipped) 18.sp else 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            if (state.flipped) {
                FlippedFooter(horizontal = 9)
            } else {
                ProgressFooter(
                    learned = state.learned,
                    total = state.total,
                    progress = state.progress,
                    caption = "${state.learned} из ${state.total} слов",
                    horizontal = 9
                )
            }
        }
    }
}

class SmallWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val settings = com.wordcards.widget.data.Settings(context)
        appWidgetIds.forEach(settings::forgetWidget)
    }
}
