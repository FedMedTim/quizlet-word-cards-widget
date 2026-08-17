package com.wordcards.widget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Accent = Color(0xFFE8321E)
val Ink = Color(0xFF111111)
val PageBg = Color(0xFFF2F1EF)
val Muted = Color(0xFF6B6B6B)

data class SetupUiState(
    val loggedIn: Boolean,
    val busy: Boolean,
    val message: String?,
    val connectedTitle: String?,
    val showAudio: Boolean
)

/**
 * Экран подключения. Тот же самый и при запуске приложения, и при добавлении
 * виджета на экран — отличается только тем, что во втором случае результат
 * возвращается лаунчеру.
 */
@Composable
fun SetupScreen(
    state: SetupUiState,
    setInput: String,
    onSetInputChange: (String) -> Unit,
    onLogin: () -> Unit,
    onSignOut: () -> Unit,
    onConnect: () -> Unit,
    onToggleAudio: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Box(
            modifier = Modifier
                .background(Accent, RoundedCornerShape(2.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "ВИДЖЕТ ДЛЯ ANDROID",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Карточки слов\nна главном экране",
            color = Ink,
            fontSize = 32.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Слово из подключённого набора Quizlet прямо на рабочем столе. " +
                "Тап переворачивает карточку и показывает перевод.",
            color = Muted,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(24.dp))

        StatusRow(state)

        Spacer(Modifier.height(20.dp))

        if (!state.loggedIn) {
            PrimaryButton(text = "Войти в Quizlet", enabled = !state.busy, onClick = onLogin)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Пароль вводится на сайте quizlet.com в системном окне. " +
                    "Приложение его не получает — сохраняется только сессия.",
                color = Muted,
                fontSize = 12.sp
            )
        } else {
            Text(
                text = "ССЫЛКА НА НАБОР",
                color = Ink,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = setInput,
                onValueChange = onSetInputChange,
                singleLine = true,
                placeholder = { Text("quizlet.com/123456789/...", color = Muted) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Откройте набор в Quizlet, нажмите «Поделиться» и вставьте ссылку сюда.",
                color = Muted,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = if (state.busy) "Загружаю карточки…" else "Подключить набор",
                enabled = !state.busy && setInput.isNotBlank(),
                onClick = onConnect
            )

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Кнопка озвучки на карточке", color = Ink, fontSize = 14.sp)
                Switch(checked = state.showAudio, onCheckedChange = onToggleAudio)
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSignOut) {
                Text("Выйти из аккаунта", color = Muted)
            }
        }

        if (state.busy) {
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
        }

        state.message?.let { text ->
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Ink, RoundedCornerShape(3.dp))
                    .padding(12.dp)
            ) {
                Text(text = text, color = Ink, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "Дальше: зажмите свободное место на рабочем столе → «Виджеты» → " +
                "«Карточки слов» и перетащите размер 2×2 или 4×2.",
            color = Muted,
            fontSize = 12.sp,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun StatusRow(state: SetupUiState) {
    val label = when {
        !state.loggedIn -> "Quizlet не подключён"
        state.connectedTitle != null -> "Подключён набор «${state.connectedTitle}»"
        else -> "Quizlet подключён"
    }
    Column {
        Box(Modifier.fillMaxWidth().height(2.dp).background(Ink))
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (state.loggedIn) "✓" else "—",
                color = if (state.loggedIn) Accent else Muted,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(Ink))
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(3.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent,
            contentColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
