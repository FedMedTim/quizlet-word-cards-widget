package com.wordcards.widget.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.wordcards.widget.quizlet.QuizletWebClient

/**
 * Окно входа в Quizlet.
 *
 * Пароль вводится на настоящей странице quizlet.com внутри WebView —
 * приложение его не видит и не хранит. Нам достаточно куки сессии, которую
 * система кладёт в общий CookieManager; оттуда её берёт фоновая синхронизация.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun QuizletLoginWeb(
    modifier: Modifier = Modifier,
    onSessionDetected: () -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        CookieManager.getInstance().flush()
                        // После удачного входа Quizlet уводит со страницы /login.
                        val leftLoginPage = url != null && !url.contains("/login")
                        if (leftLoginPage && QuizletWebClient(context).hasSessionCookies()) {
                            onSessionDetected()
                        }
                    }
                }
                loadUrl(QuizletWebClient.LOGIN_URL)
            }
        }
    )
}
