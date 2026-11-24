package com.sumizuri.app.feature.tv.profile

import com.sumizuri.app.data.profile.ProfileHost
import com.sumizuri.app.feature.onboarding.keepAvatar
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

// An anime character to use as the picture of a profile: its name and its portrait on AniList.
internal class Character(val name: String, val image: String)

// The most loved characters on AniList, which needs no key, read once while the app runs. A picked one is downloaded and kept like a photo.
internal object TvCharacters {
    private val http by lazy { HttpClient { install(HttpTimeout) { requestTimeoutMillis = 10_000 } } }
    private var read: List<Character> = emptyList()

    private const val Query =
        "query { Page(page: 1, perPage: 30) { characters(sort: FAVOURITES_DESC) { name { full } image { large } } } }"

    // Nothing when AniList cannot be reached, so the row of characters just stays away.
    suspend fun load(): List<Character> {
        if (read.isNotEmpty()) return read
        read = withContext(Dispatchers.IO) {
            runCatching {
                val answer = http.post("https://graphql.anilist.co") {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject { put("query", Query) }.toString())
                }
                if (!answer.status.isSuccess()) return@runCatching emptyList()
                val page = (Json.parseToJsonElement(answer.bodyAsText()) as? JsonObject)
                    ?.let { it["data"] as? JsonObject }
                    ?.let { it["Page"] as? JsonObject }
                (page?.get("characters") as? JsonArray).orEmpty().mapNotNull { item ->
                    val one = item as? JsonObject ?: return@mapNotNull null
                    val name = ((one["name"] as? JsonObject)?.get("full") as? JsonPrimitive)?.contentOrNull
                    val image = ((one["image"] as? JsonObject)?.get("large") as? JsonPrimitive)?.contentOrNull
                    // AniList gives a gray figure to characters with no picture of their own.
                    if (name == null || image == null || image.endsWith("default.jpg")) null else Character(name, image)
                }
            }.getOrDefault(emptyList())
        }
        return read
    }

    // The portrait downloaded, squared and kept in the avatars folder, or null when it could not be.
    suspend fun keep(host: ProfileHost, character: Character): String? = withContext(Dispatchers.IO) {
        val bytes = runCatching {
            http.get(character.image).takeIf { it.status.isSuccess() }?.bodyAsBytes()
        }.getOrNull() ?: return@withContext null
        keepAvatar(host, bytes)
    }
}
