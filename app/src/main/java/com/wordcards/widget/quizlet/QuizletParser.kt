package com.wordcards.widget.quizlet

import org.json.JSONArray
import org.json.JSONObject

/**
 * Разбор ответов внутреннего API Quizlet.
 *
 * API не документировано и обёртка вокруг данных у него менялась (responses →
 * models → studiableItem и вариации). Поэтому парсер не ходит по фиксированному
 * пути, а обходит дерево целиком и подбирает объекты по характерным признакам:
 * карточка — это объект с массивом cardSides. Такой разбор переживает смену
 * обёртки, а сломается только если поменяются сами карточки.
 */
object QuizletParser {

    fun parseTerms(payload: JSONObject): QuizletResult<List<QuizletTerm>> {
        val raw = mutableListOf<JSONObject>()
        walk(payload.opt("pages")) { node ->
            if (node.opt("cardSides") is JSONArray) raw.add(node)
        }

        val terms = raw
            .filterNot { it.optBoolean("isDeleted", false) }
            .mapNotNull { it.toTerm() }
            .distinctBy { it.id }
            .sortedBy { it.rank }

        return if (terms.isEmpty()) {
            QuizletResult.Error(Reason.EMPTY, "Quizlet вернул ответ без карточек")
        } else {
            QuizletResult.Ok(terms)
        }
    }

    fun parseSetMeta(payload: JSONObject, setId: String): QuizletResult<QuizletSetMeta> {
        var best: JSONObject? = null
        walk(payload.opt("pages")) { node ->
            val hasTitle = node.optString("title").isNotBlank()
            if (!hasTitle) return@walk
            val looksLikeSet = node.has("numTerms") || node.has("termCount") || node.has("creatorId")
            if (looksLikeSet && best == null) best = node
        }

        val node = best
            ?: return QuizletResult.Error(Reason.EMPTY, "В ответе нет данных набора")

        return QuizletResult.Ok(
            QuizletSetMeta(
                id = setId,
                title = node.optString("title").trim(),
                totalTerms = node.optInt("numTerms", node.optInt("termCount", 0))
            )
        )
    }

    /**
     * Из ссылки вида quizlet.com/ru/123456789/advanced-english-flash-cards/
     * достаёт идентификатор набора. Принимает и голый id.
     */
    fun extractSetId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.all { it.isDigit() }) return trimmed
        // Первая группа из 6+ цифр в пути — это id набора; коды локали короче.
        return Regex("""quizlet\.com/(?:[a-z]{2}/)?(\d{6,})""")
            .find(trimmed)?.groupValues?.get(1)
            ?: Regex("""(\d{6,})""").find(trimmed)?.groupValues?.get(1)
    }

    private fun JSONObject.toTerm(): QuizletTerm? {
        val sides = optJSONArray("cardSides") ?: return null
        var word: Side? = null
        var definition: Side? = null

        for (i in 0 until sides.length()) {
            val side = sides.optJSONObject(i) ?: continue
            val parsed = side.toSide() ?: continue
            when (side.optString("label")) {
                "word" -> word = parsed
                "definition" -> definition = parsed
                // Неизвестная метка: заполняем по порядку, чтобы не потерять карточку.
                else -> if (word == null) word = parsed else if (definition == null) definition = parsed
            }
        }

        val front = word ?: return null
        if (front.text.isBlank()) return null
        val back = definition ?: Side("", null, null)

        val (cleanDefinition, transcription) = splitTranscription(back.text)

        return QuizletTerm(
            id = optLong("id", optLong("studiableItemId", 0L)).takeIf { it != 0L } ?: return null,
            rank = optInt("rank", Int.MAX_VALUE),
            word = front.text,
            definition = cleanDefinition,
            transcription = transcription,
            wordTtsUrl = front.ttsUrl,
            definitionTtsUrl = back.ttsUrl,
            wordLang = front.lang,
            definitionLang = back.lang
        )
    }

    private fun JSONObject.toSide(): Side? {
        val media = optJSONArray("media") ?: return null
        for (i in 0 until media.length()) {
            val item = media.optJSONObject(i) ?: continue
            val text = item.optString("plainText").ifBlank { item.optString("richText") }
            if (text.isBlank()) continue
            return Side(
                text = text.trim(),
                ttsUrl = item.optString("ttsUrl").takeIf { it.isNotBlank() },
                lang = item.optString("languageCode").takeIf { it.isNotBlank() }
            )
        }
        return null
    }

    /**
     * Quizlet не хранит транскрипцию отдельным полем — её пишут внутрь
     * определения. Кусок в косых чертах выносим на свою строку, как в макете.
     */
    private fun splitTranscription(definition: String): Pair<String, String?> {
        val match = Regex("""/[^/\n]{2,40}/""").find(definition) ?: return definition to null
        val cleaned = definition.removeRange(match.range).trim().trim('—', '-', ',', ';').trim()
        return cleaned to match.value
    }

    private fun walk(node: Any?, onObject: (JSONObject) -> Unit) {
        when (node) {
            is JSONObject -> {
                onObject(node)
                val keys = node.keys()
                while (keys.hasNext()) walk(node.opt(keys.next()), onObject)
            }
            is JSONArray -> for (i in 0 until node.length()) walk(node.opt(i), onObject)
        }
    }

    private data class Side(val text: String, val ttsUrl: String?, val lang: String?)
}
