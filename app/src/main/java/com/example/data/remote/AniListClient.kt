package com.example.data.remote

import com.example.data.model.AiringEntry
import com.example.data.model.AnimeMedia
import com.example.data.model.NextAiringInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class DiscoverCatalog(
    val trending: List<AnimeMedia>,
    val seasonal: List<AnimeMedia>,
    val topRated: List<AnimeMedia>
)

class AniListClient(private val client: OkHttpClient) {

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val endpoint = "https://graphql.anilist.co"

    suspend fun fetchDiscoverCatalog(): Result<DiscoverCatalog> = withContext(Dispatchers.IO) {
        val query = """
            query {
              trending: Page(page: 1, perPage: 15) {
                media(type: ANIME, sort: TRENDING_DESC, isAdult: false) { ...MediaFields }
              }
              seasonal: Page(page: 1, perPage: 15) {
                media(type: ANIME, sort: POPULARITY_DESC, status: RELEASING, isAdult: false) { ...MediaFields }
              }
              topRated: Page(page: 1, perPage: 15) {
                media(type: ANIME, sort: SCORE_DESC, isAdult: false) { ...MediaFields }
              }
            }
            fragment MediaFields on Media {
              id
              idMal
              title { romaji english native }
              coverImage { extraLarge large }
              bannerImage
              description(asHtml: false)
              episodes
              duration
              status
              season
              seasonYear
              averageScore
              popularity
              format
              genres
              studios(isMain: true) { nodes { name } }
              nextAiringEpisode { episode airingAt timeUntilAiring }
              trailer { id site }
            }
        """.trimIndent()

        executeGraphQl(query).mapCatching { data ->
            val trending = data["trending"]?.jsonObject?.get("media")?.jsonArray
                ?.mapNotNull { parseMedia(it.jsonObject) } ?: emptyList()
            val seasonal = data["seasonal"]?.jsonObject?.get("media")?.jsonArray
                ?.mapNotNull { parseMedia(it.jsonObject) } ?: emptyList()
            val topRated = data["topRated"]?.jsonObject?.get("media")?.jsonArray
                ?.mapNotNull { parseMedia(it.jsonObject) } ?: emptyList()
            DiscoverCatalog(trending = trending, seasonal = seasonal, topRated = topRated)
        }
    }

    suspend fun searchAnime(queryText: String, genre: String?): Result<List<AnimeMedia>> =
        withContext(Dispatchers.IO) {
            val gql = """
                query (${'$'}search: String, ${'$'}genre: String) {
                  Page(page: 1, perPage: 25) {
                    media(type: ANIME, search: ${'$'}search, genre: ${'$'}genre, sort: POPULARITY_DESC, isAdult: false) {
                      id
                      idMal
                      title { romaji english native }
                      coverImage { extraLarge large }
                      bannerImage
                      description(asHtml: false)
                      episodes
                      duration
                      status
                      season
                      seasonYear
                      averageScore
                      popularity
                      format
                      genres
                      studios(isMain: true) { nodes { name } }
                      nextAiringEpisode { episode airingAt timeUntilAiring }
                      trailer { id site }
                    }
                  }
                }
            """.trimIndent()

            val variables = buildJsonObject {
                if (queryText.isNotBlank()) put("search", queryText.trim())
                if (!genre.isNullOrBlank() && genre != "All") put("genre", genre)
            }
            executeGraphQl(gql, variables).mapCatching { data ->
                data["Page"]?.jsonObject?.get("media")?.jsonArray
                    ?.mapNotNull { parseMedia(it.jsonObject) } ?: emptyList()
            }
        }

    suspend fun fetchAiringSchedule(startEpochSec: Long, endEpochSec: Long): Result<List<AiringEntry>> =
        withContext(Dispatchers.IO) {
            val gql = """
                query (${'$'}start: Int, ${'$'}end: Int) {
                  Page(page: 1, perPage: 50) {
                    airingSchedules(airingAt_greater: ${'$'}start, airingAt_lesser: ${'$'}end, sort: TIME) {
                      episode
                      airingAt
                      timeUntilAiring
                      media {
                        id
                        idMal
                        isAdult
                        title { romaji english native }
                        coverImage { extraLarge large }
                        bannerImage
                        description(asHtml: false)
                        episodes
                        duration
                        status
                        season
                        seasonYear
                        averageScore
                        popularity
                        format
                        genres
                        studios(isMain: true) { nodes { name } }
                        nextAiringEpisode { episode airingAt timeUntilAiring }
                        trailer { id site }
                      }
                    }
                  }
                }
            """.trimIndent()

            val variables = buildJsonObject {
                put("start", startEpochSec)
                put("end", endEpochSec)
            }
            executeGraphQl(gql, variables).mapCatching { data ->
                data["Page"]?.jsonObject?.get("airingSchedules")?.jsonArray
                    ?.mapNotNull { elem ->
                        val obj = elem.jsonObject
                        val mediaObj = obj["media"]?.jsonObject ?: return@mapNotNull null
                        if (mediaObj["isAdult"]?.jsonPrimitive?.content == "true") return@mapNotNull null
                        val media = parseMedia(mediaObj) ?: return@mapNotNull null
                        val episode = obj["episode"]?.jsonPrimitive?.intOrNull ?: 1
                        val airingAt = obj["airingAt"]?.jsonPrimitive?.longOrNull ?: 0L
                        val until = obj["timeUntilAiring"]?.jsonPrimitive?.longOrNull ?: 0L
                        AiringEntry(media, episode, airingAt, until)
                    } ?: emptyList()
            }
        }

    suspend fun fetchAnimeDetails(animeId: Int): Result<AnimeMedia> = withContext(Dispatchers.IO) {
        val gql = """
            query (${'$'}id: Int) {
              Media(id: ${'$'}id, type: ANIME) {
                id
                idMal
                title { romaji english native }
                coverImage { extraLarge large }
                bannerImage
                description(asHtml: false)
                episodes
                duration
                status
                season
                seasonYear
                averageScore
                popularity
                format
                genres
                studios(isMain: true) { nodes { name } }
                nextAiringEpisode { episode airingAt timeUntilAiring }
                trailer { id site }
              }
            }
        """.trimIndent()

        val variables = buildJsonObject { put("id", animeId) }
        executeGraphQl(gql, variables).mapCatching { data ->
            val mediaObj = data["Media"]?.jsonObject
                ?: throw IllegalStateException("Anime #$animeId not found on AniList")
            parseMedia(mediaObj) ?: throw IllegalStateException("Malformed AniList response")
        }
    }

    private fun executeGraphQl(
        query: String,
        variables: JsonObject? = null
    ): Result<JsonObject> {
        return runCatching {
            val payload = buildJsonObject {
                put("query", query)
                if (variables != null) put("variables", variables)
            }
            val reqBuilder = Request.Builder()
                .url(endpoint)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "Maddookie/1.0.1")

            client.newCall(reqBuilder.build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException("AniList HTTP ${response.code}: ${body.take(160)}")
                }
                val root = json.parseToJsonElement(body).jsonObject
                val errors = root["errors"]
                if (errors is JsonArray && errors.isNotEmpty()) {
                    val msg = errors.first().jsonObject["message"]?.jsonPrimitive?.content ?: "GraphQL error"
                    throw IllegalStateException(msg)
                }
                root["data"]?.jsonObject
                    ?: throw IllegalStateException("Empty AniList GraphQL data payload")
            }
        }
    }

    private fun parseMedia(obj: JsonObject): AnimeMedia? {
        val id = obj["id"]?.jsonPrimitive?.intOrNull ?: return null
        val titleObj = obj["title"]?.jsonObject ?: return null
        val romaji = titleObj["romaji"]?.strOrNull() ?: "Unknown Anime"
        val english = titleObj["english"]?.strOrNull()
        val nativeTitle = titleObj["native"]?.strOrNull()

        val coverObj = obj["coverImage"]?.jsonObject
        val coverUrl = coverObj?.get("extraLarge")?.strOrNull()
            ?: coverObj?.get("large")?.strOrNull()
            ?: ""

        val nextObj = obj["nextAiringEpisode"]?.let { if (it is JsonObject) it else null }
        val nextAiring = nextObj?.let {
            val ep = it["episode"]?.jsonPrimitive?.intOrNull
            val at = it["airingAt"]?.jsonPrimitive?.longOrNull
            val until = it["timeUntilAiring"]?.jsonPrimitive?.longOrNull
            if (ep != null && at != null && until != null) NextAiringInfo(ep, at, until) else null
        }

        val trailerObj = obj["trailer"]?.let { if (it is JsonObject) it else null }
        val trailerId = trailerObj?.get("id")?.strOrNull()
        val trailerSite = trailerObj?.get("site")?.strOrNull()
        val trailerUrl = if (trailerId != null && trailerSite == "youtube") {
            "https://www.youtube.com/watch?v=$trailerId"
        } else null

        val genres = (obj["genres"] as? JsonArray)?.mapNotNull { it.strOrNull() } ?: emptyList()
        val studios = obj["studios"]?.jsonObject?.get("nodes")?.let { it as? JsonArray }
            ?.mapNotNull { it.jsonObject["name"]?.strOrNull() } ?: emptyList()

        val rawDesc = obj["description"]?.strOrNull().orEmpty()
        val cleanDesc = rawDesc
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#039;", "'")
            .trim()

        return AnimeMedia(
            id = id,
            malId = obj["idMal"]?.jsonPrimitive?.intOrNull,
            titleRomaji = romaji,
            titleEnglish = english,
            titleNative = nativeTitle,
            coverUrl = coverUrl,
            bannerUrl = obj["bannerImage"]?.strOrNull(),
            description = cleanDesc,
            episodes = obj["episodes"]?.jsonPrimitive?.intOrNull,
            durationMinutes = obj["duration"]?.jsonPrimitive?.intOrNull,
            status = obj["status"]?.strOrNull(),
            season = obj["season"]?.strOrNull(),
            seasonYear = obj["seasonYear"]?.jsonPrimitive?.intOrNull,
            averageScore = obj["averageScore"]?.jsonPrimitive?.intOrNull,
            popularity = obj["popularity"]?.jsonPrimitive?.intOrNull ?: 0,
            format = obj["format"]?.strOrNull(),
            genres = genres,
            studios = studios,
            nextAiringEpisode = nextAiring,
            trailerUrl = trailerUrl
        )
    }

    private fun JsonElement.strOrNull(): String? {
        val p = this as? JsonPrimitive ?: return null
        if (p.isString) return p.content.takeIf { it.isNotBlank() && it != "null" }
        return null
    }
}
