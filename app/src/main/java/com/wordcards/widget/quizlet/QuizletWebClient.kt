package com.wordcards.widget.quizlet

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

/**
 * Доступ к данным Quizlet.
 *
 * Прямой HTTP-запрос к quizlet.com из OkHttp получает 403: сайт закрыт защитой
 * от ботов, которая смотрит на TLS-отпечаток и заголовки клиента. Поэтому
 * запросы идут изнутри WebView — он уже прошёл проверку при логине, держит
 * сессионные куки и по всем признакам является обычным браузером.
 *
 * WebView не привязан к окну: он нужен только как JS-движок с правильным
 * origin, ничего не рендерится.
 */
class QuizletWebClient(context: Context) {

    private val appContext = context.applicationContext

    /** Есть ли следы залогиненной сессии. Окончательный ответ даёт только запрос. */
    fun hasSessionCookies(): Boolean {
        val cookies = CookieManager.getInstance().getCookie(ORIGIN) ?: return false
        return SESSION_COOKIES.any { cookies.contains("$it=") }
    }

    fun clearSession() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    /**
     * Карточки набора. Пагинация внутри JS, чтобы не гонять WebView по кругу.
     */
    suspend fun fetchTerms(setId: String): QuizletResult<List<QuizletTerm>> {
        val js = """
            (async function () {
              try {
                var all = [], page = 1, token = null;
                while (page <= 40) {
                  var u = "$ORIGIN/webapi/3.4/studiable-item-documents"
                    + "?filters%5BstudiableContainerId%5D=" + ${setId.jsString()}
                    + "&filters%5BstudiableContainerType%5D=1"
                    + "&perPage=200&page=" + page
                    + (token ? "&pagingToken=" + encodeURIComponent(token) : "");
                  var r = await fetch(u, {
                    credentials: "include",
                    headers: { "Accept": "application/json" }
                  });
                  if (!r.ok) { done({ error: "http", status: r.status }); return; }
                  var j = await r.json();
                  all.push(j);
                  var resp = (j.responses && j.responses[0]) || j;
                  var paging = resp.paging || {};
                  if (!paging.token || paging.token === token) break;
                  token = paging.token;
                  page++;
                }
                done({ ok: true, pages: all });
              } catch (e) {
                done({ error: "js", message: String(e) });
              }
            })();
        """.trimIndent()

        return runScript(js).map { payload ->
            QuizletParser.parseTerms(payload)
        }
    }

    /** Название и размер набора — для плашки-заголовка и строки прогресса. */
    suspend fun fetchSetMeta(setId: String): QuizletResult<QuizletSetMeta> {
        val js = """
            (async function () {
              try {
                var r = await fetch("$ORIGIN/webapi/3.2/sets/" + ${setId.jsString()}, {
                  credentials: "include",
                  headers: { "Accept": "application/json" }
                });
                if (!r.ok) { done({ error: "http", status: r.status }); return; }
                done({ ok: true, pages: [await r.json()] });
              } catch (e) {
                done({ error: "js", message: String(e) });
              }
            })();
        """.trimIndent()

        return runScript(js).map { payload ->
            QuizletParser.parseSetMeta(payload, setId)
        }
    }

    /**
     * Грузит origin quizlet.com, выполняет скрипт и ждёт ответа через мост.
     *
     * Сначала пробуется robots.txt — это несколько сотен байт с нужным origin.
     * Если защита от ботов отдаёт 403, страница перезагружается уже полноценная:
     * она проходит JS-проверку и обновляет cf-куку, после чего запрос повторяется.
     */
    private suspend fun runScript(script: String): QuizletResult<JSONObject> {
        val first = loadAndRun(LIGHT_PAGE, script)
        if (first is QuizletResult.Error && first.reason == Reason.FORBIDDEN) {
            return loadAndRun(FULL_PAGE, script)
        }
        return first
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun loadAndRun(url: String, script: String): QuizletResult<JSONObject> =
        withContext(Dispatchers.Main) {
            val nonce = UUID.randomUUID().toString()
            val result = CompletableDeferred<String>()
            val pageLoaded = CompletableDeferred<Unit>()
            var webView: WebView? = null

            try {
                withTimeout(TIMEOUT_MS) {
                    val view = WebView(appContext)
                    webView = view
                    view.settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                    }
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(view, true)

                    view.addJavascriptInterface(Bridge(nonce, result), BRIDGE_NAME)
                    view.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            if (!pageLoaded.isCompleted) pageLoaded.complete(Unit)
                        }
                    }
                    view.loadUrl(url)
                    pageLoaded.await()

                    view.evaluateJavascript(wrap(script, nonce), null)
                    val payload = result.await()
                    parseEnvelope(payload)
                }
            } catch (e: TimeoutCancellationException) {
                QuizletResult.Error(Reason.TIMEOUT, "Quizlet не ответил за ${TIMEOUT_MS / 1000} с")
            } catch (e: Exception) {
                QuizletResult.Error(Reason.UNKNOWN, e.message ?: e.javaClass.simpleName)
            } finally {
                webView?.let { view ->
                    view.removeJavascriptInterface(BRIDGE_NAME)
                    view.stopLoading()
                    view.destroy()
                }
            }
        }

    private fun parseEnvelope(payload: String): QuizletResult<JSONObject> {
        val json = try {
            JSONObject(payload)
        } catch (e: Exception) {
            return QuizletResult.Error(Reason.UNKNOWN, "Ответ не разобран: ${payload.take(120)}")
        }
        if (json.optBoolean("ok")) return QuizletResult.Ok(json)

        return when (json.optString("error")) {
            "http" -> when (val status = json.optInt("status")) {
                401, 403 -> QuizletResult.Error(Reason.FORBIDDEN, "Quizlet ответил $status")
                404 -> QuizletResult.Error(Reason.NOT_FOUND, "Набор не найден")
                else -> QuizletResult.Error(Reason.UNKNOWN, "HTTP $status")
            }
            else -> QuizletResult.Error(Reason.UNKNOWN, json.optString("message", "Неизвестная ошибка"))
        }
    }

    private fun wrap(script: String, nonce: String) = """
        (function () {
          var __n = ${nonce.jsString()};
          window.done = function (o) {
            try { $BRIDGE_NAME.deliver(__n, JSON.stringify(o)); } catch (e) {}
          };
          $script
        })();
    """.trimIndent()

    /**
     * Мост из JS в Kotlin. Nonce отсекает вызовы из посторонних фреймов
     * страницы: доверять мы можем только скрипту, который внедрили сами.
     */
    private class Bridge(
        private val nonce: String,
        private val target: CompletableDeferred<String>
    ) {
        @JavascriptInterface
        fun deliver(token: String, payload: String) {
            if (token == nonce) target.complete(payload)
        }
    }

    companion object {
        const val ORIGIN = "https://quizlet.com"
        const val LOGIN_URL = "$ORIGIN/login"

        private const val LIGHT_PAGE = "$ORIGIN/robots.txt"
        private const val FULL_PAGE = "$ORIGIN/latest"
        private const val BRIDGE_NAME = "WordCardsBridge"
        private const val TIMEOUT_MS = 60_000L
        private val SESSION_COOKIES = listOf("qi", "fubar", "qtkn")
    }
}

private fun String.jsString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "") + "\""
