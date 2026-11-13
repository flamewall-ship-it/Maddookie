package com.example.data.remote

import com.example.data.model.AnimeMedia
import com.example.data.model.ProviderProbeState
import com.example.data.model.ResolvedStream
import com.example.data.model.StreamAudioMode
import com.example.data.model.StreamProvider
import com.example.data.model.StreamResolutionAttempt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class MultiProviderStreamingEngine(private val client: OkHttpClient) {

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun resolveEpisodeStream(
        anime: AnimeMedia,
        episode: Int,
        audioMode: StreamAudioMode,
        preferredProvider: StreamProvider? = null,
        onAttemptUpdate: (List<StreamResolutionAttempt>) -> Unit
    ): Result<ResolvedStream> = withContext(Dispatchers.IO) {
        val orderedProviders = buildList {
            if (preferredProvider != null) add(preferredProvider)
            StreamProvider.entries.forEach { if (it != preferredProvider) add(it) }
        }

        val attempts = orderedProviders.map {
            StreamResolutionAttempt(it, ProviderProbeState.PENDING, "Queued")
        }.toMutableList()
        onAttemptUpdate(attempts.toList())

        for (index in orderedProviders.indices) {
            val provider = orderedProviders[index]
            attempts[index] = StreamResolutionAttempt(
                provider = provider,
                state = ProviderProbeState.CHECKING,
                detail = "Resolving Ep $episode (${audioMode.label})..."
            )
            onAttemptUpdate(attempts.toList())

            val startNs = System.nanoTime()
            val resolved = when (provider) {
                StreamProvider.ALLANIME -> resolveFromAllAnime(anime, episode, audioMode)
                StreamProvider.ANIMEPAHE -> probeFallbackProvider(provider, anime, episode)
                StreamProvider.ANIKOTO -> probeFallbackProvider(provider, anime, episode)
                StreamProvider.ANIMEKAI -> probeFallbackProvider(provider, anime, episode)
                StreamProvider.KICKASSANIME -> probeFallbackProvider(provider, anime, episode)
            }
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L

            if (resolved.isSuccess) {
                val stream = resolved.getOrThrow()
                attempts[index] = StreamResolutionAttempt(
                    provider = provider,
                    state = ProviderProbeState.SUCCESS,
                    detail = "Locked ${stream.quality} ${if (stream.isHls) "HLS" else "MP4"}",
                    latencyMs = elapsedMs
                )
                onAttemptUpdate(attempts.toList())
                return@withContext Result.success(stream.copy(attempts = attempts.toList()))
            } else {
                val reason = resolved.exceptionOrNull()?.message ?: "No playable source"
                attempts[index] = StreamResolutionAttempt(
                    provider = provider,
                    state = ProviderProbeState.FAILED,
                    detail = reason,
                    latencyMs = elapsedMs
                )
                onAttemptUpdate(attempts.toList())
            }
        }

        Result.failure(
            IllegalStateException(
                "All 5 providers (${orderedProviders.joinToString(" -> ") { it.displayName }}) failed to return a direct HLS/MP4 stream for Episode $episode (${audioMode.label})."
            )
        )
    }

    private fun resolveFromAllAnime(
        anime: AnimeMedia,
        episode: Int,
        audioMode: StreamAudioMode
    ): Result<ResolvedStream> {
        return runCatching {
            val searchQuery = """
                query (${'$'}search: SearchInput, ${'$'}limit: Int, ${'$'}page: Int, ${'$'}translationType: VaildTranslationTypeEnumType, ${'$'}countryOrigin: VaildCountryOriginEnumType) {
                  shows(search: ${'$'}search, limit: ${'$'}limit, page: ${'$'}page, translationType: ${'$'}translationType, countryOrigin: ${'$'}countryOrigin) {
                    edges { _id name englishName aniListId availableEpisodes }
                  }
                }
            """.trimIndent()

            val searchVars = buildJsonObject {
                put("search", buildJsonObject {
                    put("allowAdult", false)
                    put("allowUnknown", false)
                    put("query", anime.titleRomaji)
                })
                put("limit", 10)
                put("page", 1)
                put("translationType", audioMode.apiKey)
                put("countryOrigin", "ALL")
            }

            val searchPayload = buildJsonObject {
                put("query", searchQuery)
                put("variables", searchVars)
            }
            val searchReq = Request.Builder()
                .url(StreamProvider.ALLANIME.baseUrl)
                .post(searchPayload.toString().toRequestBody(jsonMediaType))
                .header("Referer", "https://allanime.to")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            val showId = client.newCall(searchReq).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                val body = resp.body?.string().orEmpty()
                val edges = json.parseToJsonElement(body).jsonObject["data"]?.jsonObject
                    ?.get("shows")?.jsonObject?.get("edges")?.jsonArray
                    ?: throw IllegalStateException("No matches")

                val exactIdMatch = edges.firstOrNull {
                    it.jsonObject["aniListId"]?.jsonPrimitive?.content == anime.id.toString()
                } ?: edges.firstOrNull()
                    ?: throw IllegalStateException("Title not indexed")

                exactIdMatch.jsonObject["_id"]?.jsonPrimitive?.content
                    ?: throw IllegalStateException("Missing show ID")
            }

            val epQuery = """
                query (${'$'}showId: String!, ${'$'}translationType: VaildTranslationTypeEnumType!, ${'$'}episodeString: String!) {
                  episode(showId: ${'$'}showId, translationType: ${'$'}translationType, episodeString: ${'$'}episodeString) {
                    episodeString
                    sourceUrls
                  }
                }
            """.trimIndent()
            val epVars = buildJsonObject {
                put("showId", showId)
                put("translationType", audioMode.apiKey)
                put("episodeString", episode.toString())
            }
            val epPayload = buildJsonObject {
                put("query", epQuery)
                put("variables", epVars)
            }
            val epReq = Request.Builder()
                .url(StreamProvider.ALLANIME.baseUrl)
                .post(epPayload.toString().toRequestBody(jsonMediaType))
                .header("Referer", "https://allanime.to")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            val sourceUrls = client.newCall(epReq).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("Episode HTTP ${resp.code}")
                val body = resp.body?.string().orEmpty()
                json.parseToJsonElement(body).jsonObject["data"]?.jsonObject
                    ?.get("episode")?.jsonObject?.get("sourceUrls") as? JsonArray
                    ?: throw IllegalStateException("Episode $episode unavailable (${audioMode.label})")
            }

            val candidateClockPaths = sourceUrls.mapNotNull { entry ->
                val rawUrl = entry.jsonObject["sourceUrl"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val sourceName = entry.jsonObject["sourceName"]?.jsonPrimitive?.content ?: "Source"
                val decoded = decodeAllAnimeSource(rawUrl)
                if (decoded.startsWith("http")) {
                    Triple(sourceName, decoded, true)
                } else if (decoded.startsWith("/apivtwo/clock")) {
                    Triple(sourceName, "https://allanime.day${decoded.replace("clock", "clock.json")}", false)
                } else null
            }

            for ((sourceName, url, isDirect) in candidateClockPaths) {
                if (isDirect && (url.contains(".m3u8") || url.contains(".mp4"))) {
                    return@runCatching ResolvedStream(
                        url = url,
                        isHls = url.contains(".m3u8"),
                        quality = sourceName,
                        provider = StreamProvider.ALLANIME,
                        audioMode = audioMode,
                        headers = mapOf("Referer" to "https://allanime.to")
                    )
                }
                if (!isDirect) {
                    val clockReq = Request.Builder()
                        .url(url)
                        .header("Referer", "https://allanime.to")
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                        .build()
                    val resolvedLink = runCatching {
                        client.newCall(clockReq).execute().use { resp ->
                            if (!resp.isSuccessful) return@use null
                            val body = resp.body?.string().orEmpty()
                            val links = json.parseToJsonElement(body).jsonObject["links"]?.jsonArray
                                ?: return@use null
                            links.firstNotNullOfOrNull { linkObj ->
                                val link = linkObj.jsonObject["link"]?.jsonPrimitive?.content
                                val hls = linkObj.jsonObject["hls"]?.jsonPrimitive?.content == "true"
                                val res = linkObj.jsonObject["resolutionStr"]?.jsonPrimitive?.content ?: "Auto"
                                if (!link.isNullOrBlank() && link.startsWith("http")) {
                                    Triple(link, hls || link.contains(".m3u8"), res)
                                } else null
                            }
                        }
                    }.getOrNull()

                    if (resolvedLink != null) {
                        return@runCatching ResolvedStream(
                            url = resolvedLink.first,
                            isHls = resolvedLink.second,
                            quality = resolvedLink.third,
                            provider = StreamProvider.ALLANIME,
                            audioMode = audioMode,
                            headers = mapOf("Referer" to "https://allanime.to")
                        )
                    }
                }
            }
            throw IllegalStateException("No direct HLS/MP4 stream extracted")
        }
    }

    private fun probeFallbackProvider(
        provider: StreamProvider,
        anime: AnimeMedia,
        episode: Int
    ): Result<ResolvedStream> {
        return runCatching {
            val slug = anime.titleRomaji.lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
            val probeUrl = when (provider) {
                StreamProvider.ANIMEPAHE -> "${provider.baseUrl}/api?m=search&q=${java.net.URLEncoder.encode(anime.titleRomaji, "UTF-8")}"
                StreamProvider.ANIKOTO -> "${provider.baseUrl}/watch/$slug-episode-$episode"
                StreamProvider.ANIMEKAI -> "${provider.baseUrl}/browser?keyword=${java.net.URLEncoder.encode(anime.titleRomaji, "UTF-8")}"
                StreamProvider.KICKASSANIME -> "${provider.baseUrl}/api/search"
                StreamProvider.ALLANIME -> provider.baseUrl
            }
            val req = Request.Builder()
                .url(probeUrl)
                .get()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("HTTP ${resp.code} challenge")
                }
                val body = resp.body?.string().orEmpty()
                val streamMatch = Regex("https?://[^\"'\\s<>]+\\.(?:m3u8|mp4)(?:\\?[^\"'\\s<>]*)?")
                    .find(body)?.value
                if (streamMatch != null) {
                    ResolvedStream(
                        url = streamMatch,
                        isHls = streamMatch.contains(".m3u8"),
                        quality = "Auto",
                        provider = provider,
                        audioMode = StreamAudioMode.SUB,
                        headers = mapOf("Referer" to provider.baseUrl)
                    )
                } else {
                    throw IllegalStateException("JS/WebView token required")
                }
            }
        }
    }

    private fun decodeAllAnimeSource(encoded: String): String {
        if (!encoded.startsWith("--")) return encoded
        val hex = encoded.removePrefix("--")
        if (hex.length % 2 != 0) return encoded
        return buildString {
            for (i in hex.indices step 2) {
                val byteVal = hex.substring(i, i + 2).toIntOrNull(16) ?: return encoded
                append((byteVal xor 0x38).toChar())
            }
        }
    }
}
