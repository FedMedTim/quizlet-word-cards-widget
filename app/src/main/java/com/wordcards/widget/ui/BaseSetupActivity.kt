package com.wordcards.widget.ui

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.wordcards.widget.data.AppDatabase
import com.wordcards.widget.data.Settings
import com.wordcards.widget.quizlet.QuizletParser
import com.wordcards.widget.quizlet.QuizletRepository
import com.wordcards.widget.quizlet.QuizletResult
import com.wordcards.widget.quizlet.Reason
import com.wordcards.widget.sync.SyncScheduler
import com.wordcards.widget.widget.WidgetUpdater
import kotlinx.coroutines.launch

/**
 * Общая логика двух входов в настройку: из лаунчера приложения и из диалога
 * добавления виджета. Различаются только тем, что делать после подключения
 * набора.
 */
abstract class BaseSetupActivity : ComponentActivity() {

    /** Экземпляр виджета, который настраиваем, либо INVALID для запуска из меню. */
    protected open val targetWidgetId: Int
        get() = AppWidgetManager.INVALID_APPWIDGET_ID

    protected open fun onSetConnected(setId: String) = Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SetupFlow(
                        widgetId = targetWidgetId,
                        onConnected = ::onSetConnected
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupFlow(widgetId: Int, onConnected: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val repository = remember { QuizletRepository(context) }
    val settings = remember { Settings(context) }
    val dao = remember { AppDatabase.get(context).wordDao() }

    var loggedIn by remember { mutableStateOf(repository.hasSession()) }
    var showLogin by remember { mutableStateOf(false) }
    var setInput by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var connectedTitle by remember { mutableStateOf<String?>(null) }
    var showAudio by remember { mutableStateOf(settings.showAudioButton) }

    LaunchedEffect(Unit) {
        settings.defaultSetId?.let { id ->
            connectedTitle = dao.getSet(id)?.title
            setInput = id
        }
    }

    if (showLogin) {
        QuizletLoginWeb(
            modifier = Modifier.fillMaxSize(),
            onSessionDetected = {
                showLogin = false
                loggedIn = true
                message = "Вход выполнен. Теперь вставьте ссылку на набор."
            }
        )
        return
    }

    SetupScreen(
        state = SetupUiState(
            loggedIn = loggedIn,
            busy = busy,
            message = message,
            connectedTitle = connectedTitle,
            showAudio = showAudio
        ),
        setInput = setInput,
        onSetInputChange = { setInput = it; message = null },
        onLogin = { message = null; showLogin = true },
        onSignOut = {
            repository.signOut()
            loggedIn = false
            connectedTitle = null
            message = "Сессия Quizlet удалена."
        },
        onToggleAudio = { enabled ->
            showAudio = enabled
            settings.showAudioButton = enabled
            scope.launch { WidgetUpdater.refreshAll(context) }
        },
        onConnect = onConnect@{
            val setId = QuizletParser.extractSetId(setInput)
            if (setId == null) {
                message = "Не нашёл номер набора в ссылке. Нужна ссылка вида " +
                    "quizlet.com/123456789/название/"
                return@onConnect
            }

            busy = true
            message = null
            scope.launch {
                when (val result = repository.syncSet(setId)) {
                    is QuizletResult.Ok -> {
                        settings.defaultSetId = setId
                        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            settings.bindWidget(widgetId, setId)
                        }
                        connectedTitle = result.value.title
                        message = "Загружено карточек: ${result.value.totalTerms}."
                        SyncScheduler.ensureScheduled(context)
                        WidgetUpdater.refreshAll(context)
                        busy = false
                        onConnected(setId)
                    }

                    is QuizletResult.Error -> {
                        busy = false
                        if (result.reason == Reason.FORBIDDEN) loggedIn = false
                        message = when (result.reason) {
                            Reason.FORBIDDEN ->
                                "Quizlet не пустил к набору. Войдите в аккаунт заново."
                            Reason.NOT_FOUND ->
                                "Набор с таким номером не найден."
                            Reason.EMPTY ->
                                "Ответ получен, но карточек в нём нет. Возможно, " +
                                    "Quizlet поменял формат ответа."
                            Reason.TIMEOUT ->
                                "Quizlet не ответил вовремя. Попробуйте ещё раз."
                            Reason.UNKNOWN ->
                                "Не получилось: ${result.message}"
                        }
                    }
                }
            }
        }
    )
}
