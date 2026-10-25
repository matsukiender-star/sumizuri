package com.sumizuri.app.feature.reader.lookup

import com.sumizuri.app.data.model.reader.ReaderLookup
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLParameter
import io.ktor.http.encodeURLPathPart
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val English = "wiktionary.org"
private const val Japanese = "jisho.org"

// Where each dictionary is asked. The word goes at the end.
private const val WiktionaryAddress = "https://en.wiktionary.org/api/rest_v1/page/definition/"
private const val JishoAddress = "https://jisho.org/api/v1/search/words?keyword="

// The web search a word can be opened in when the dictionaries have nothing.
private const val WebSearchAddress = "https://www.google.com/search?q="

// Wiktionary asks to be told who is calling.
private const val Caller = "Sumizuri"

// How long a dictionary has to answer.
private const val AnswerWithinMs = 8_000L

// The web search for the meaning of a word.
internal fun webSearchFor(word: String): String = WebSearchAddress + "define $word".encodeURLParameter()

// Asks free online dictionaries for the meaning of a word. English comes from Wiktionary and Japanese from Jisho. Answers are kept while the client lives.
class DictionaryClient : AutoCloseable {
    private val http = HttpClient { install(HttpTimeout) { requestTimeoutMillis = AnswerWithinMs } }
    private val cache = HashMap<String, LookupResult>()

    suspend fun look(query: String, mode: ReaderLookup): LookupResult {
        val japanese = when (mode) {
            ReaderLookup.Japanese -> true
            ReaderLookup.English -> false
            else -> query.firstOrNull()?.let(WordPick::isCjk) == true
        }
        val key = (if (japanese) "ja:" else "en:") + query.lowercase()
        cache[key]?.let { return it }
        val result = try {
            if (japanese) japanese(query) else english(query.lowercase())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            LookupResult.Failed
        }
        if (result !is LookupResult.Failed) cache[key] = result
        return result
    }

    // Wiktionary lists the plain form and also says what an ending form is of, so a plural or a past tense finds its word.
    private suspend fun english(word: String): LookupResult {
        val response = http.get(WiktionaryAddress + word.encodeURLPathPart()) { header(HttpHeaders.UserAgent, Caller) }
        if (response.status == HttpStatusCode.NotFound) return LookupResult.Missing
        if (response.status != HttpStatusCode.OK) return LookupResult.Failed
        val sections = Json
            .parseToJsonElement(response.bodyAsText())
            .jsonObject["en"]
            ?.jsonArray
            .orEmpty()
        val meanings = sections
            .map { section ->
                val body = section.jsonObject
                Meaning(
                    body["partOfSpeech"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    body["definitions"]
                        ?.jsonArray
                        .orEmpty()
                        .take(3)
                        .mapNotNull {
                            plainText(
                                it.jsonObject["definition"]
                                    ?.jsonPrimitive
                                    ?.contentOrNull
                                    .orEmpty(),
                            ).ifBlank { null }
                        },
                )
            }.filter { it.definitions.isNotEmpty() }
        return if (meanings.isEmpty()) {
            LookupResult.Missing
        } else {
            LookupResult.Found(
                listOf(DictionaryEntry(word, null, meanings)),
                English,
            )
        }
    }

    // A definition comes as a piece of a web page, so its tags are dropped and its escapes turned back into letters.
    private fun plainText(html: String): String = html
        .replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .trim()

    // The longest start of the characters that Jisho knows, so a word followed by the next one is still found.
    private suspend fun japanese(run: String): LookupResult {
        var failed = false
        for (length in run.length downTo 1) {
            val response = http.get(JishoAddress + run.take(length).encodeURLParameter())
            if (response.status != HttpStatusCode.OK) {
                failed = true
                continue
            }
            val data = Json
                .parseToJsonElement(response.bodyAsText())
                .jsonObject["data"]
                ?.jsonArray
                .orEmpty()
            val entries = data.take(3).map { japaneseEntry(it.jsonObject) }.filter { it.meanings.isNotEmpty() }
            if (entries.isNotEmpty()) return LookupResult.Found(entries, Japanese)
        }
        return if (failed) LookupResult.Failed else LookupResult.Missing
    }

    private fun japaneseEntry(entry: JsonObject): DictionaryEntry {
        val spelling = entry["japanese"]?.jsonArray?.firstOrNull()?.jsonObject
        val written = spelling?.get("word")?.jsonPrimitive?.contentOrNull
        val reading = spelling?.get("reading")?.jsonPrimitive?.contentOrNull
        val meanings = entry["senses"]
            ?.jsonArray
            .orEmpty()
            .take(4)
            .map { sense ->
                val body = sense.jsonObject
                Meaning(
                    body["parts_of_speech"]
                        ?.jsonArray
                        .orEmpty()
                        .mapNotNull { it.jsonPrimitive.contentOrNull }
                        .joinToString(", "),
                    listOf(
                        body["english_definitions"]
                            ?.jsonArray
                            .orEmpty()
                            .mapNotNull { it.jsonPrimitive.contentOrNull }
                            .joinToString("; "),
                    ),
                )
            }.filter { it.definitions.single().isNotBlank() }
        return DictionaryEntry(written ?: reading.orEmpty(), if (written == null) null else reading, meanings)
    }

    override fun close() = http.close()
}

// The meanings of one kind of word, such as a noun, each as a line.
class Meaning(val partOfSpeech: String, val definitions: List<String>)

// One word of the dictionary with how it is read, when that differs from how it is written.
class DictionaryEntry(val word: String, val reading: String?, val meanings: List<Meaning>)

// What the dictionary said to a word: its entries and where they came from, or that it has none, or that it could not be asked.
sealed interface LookupResult {
    class Found(val entries: List<DictionaryEntry>, val source: String) : LookupResult

    data object Missing : LookupResult

    data object Failed : LookupResult
}
