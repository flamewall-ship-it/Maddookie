package com.example.data.model

import kotlinx.serialization.Serializable

enum class WatchStatus(val label: String, val anilistValue: String, val malValue: String) {
    WATCHING("Watching", "CURRENT", "watching"),
    PLAN_TO_WATCH("Plan to Watch", "PLANNING", "plan_to_watch"),
    COMPLETED("Completed", "COMPLETED", "completed"),
    ON_HOLD("On Hold", "PAUSED", "on_hold"),
    DROPPED("Dropped", "DROPPED", "dropped")
}

enum class StreamProvider(val displayName: String, val baseUrl: String) {
    ALLANIME("AllAnime", "https://api.allanime.day/api"),
    ANIMEPAHE("AnimePahe", "https://animepahe.ru"),
    ANIKOTO("Anikoto", "https://anikoto.to"),
    ANIMEKAI("AnimeKai", "https://animekai.to"),
    KICKASSANIME("KickAssAnime", "https://kickassanime.mx")
}

enum class StreamAudioMode(val label: String, val apiKey: String) {
    SUB("SUB", "sub"),
    DUB("DUB", "dub")
}

enum class ProviderProbeState {
    PENDING,
    CHECKING,
    SUCCESS,
    FAILED
}

data class StreamResolutionAttempt(
    val provider: StreamProvider,
    val state: ProviderProbeState,
    val detail: String,
    val latencyMs: Long = 0L
)

data class ResolvedStream(
    val url: String,
    val isHls: Boolean,
    val quality: String,
    val provider: StreamProvider,
    val audioMode: StreamAudioMode,
    val headers: Map<String, String> = emptyMap(),
    val attempts: List<StreamResolutionAttempt> = emptyList(),
    val isTrailerFallback: Boolean = false
)

@Serializable
data class NextAiringInfo(
    val episode: Int,
    val airingAt: Long,
    val timeUntilAiring: Long
)

@Serializable
data class AnimeMedia(
    val id: Int,
    val malId: Int? = null,
    val titleRomaji: String,
    val titleEnglish: String? = null,
    val titleNative: String? = null,
    val coverUrl: String,
    val bannerUrl: String? = null,
    val description: String = "",
    val episodes: Int? = null,
    val durationMinutes: Int? = null,
    val status: String? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val averageScore: Int? = null,
    val popularity: Int = 0,
    val format: String? = null,
    val genres: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
    val nextAiringEpisode: NextAiringInfo? = null,
    val trailerUrl: String? = null
) {
    val displayTitle: String
        get() = titleEnglish?.takeIf { it.isNotBlank() } ?: titleRomaji
}

data class AiringEntry(
    val media: AnimeMedia,
    val episode: Int,
    val airingAt: Long,
    val timeUntilAiring: Long
)

data class PotatoTurboConfig(
    val rgb565Enabled: Boolean = true,
    val heapCapPercent: Int = 12,
    val deferredWebViewEnabled: Boolean = true,
    val zeroBlurFastPath: Boolean = true,
    val darkTheme: Boolean = true
)

data class SyncAccountState(
    val anilistConnected: Boolean = false,
    val anilistUsername: String = "",
    val anilistToken: String = "",
    val anilistClientId: String = "",
    val malConnected: Boolean = false,
    val malUsername: String = "",
    val malToken: String = "",
    val malClientId: String = "",
    val simklConnected: Boolean = false,
    val simklClientId: String = "",
    val simklUserCode: String = "",
    val simklVerificationUrl: String = "https://simkl.com/pin",
    val simklPinExpiresAt: Long = 0L,
    val simklToken: String = "",
    val autoSyncAt85Percent: Boolean = true
)

data class DiscordRpcConfig(
    val enabled: Boolean = true,
    val showEpisodeProgress: Boolean = true,
    val showProviderBadge: Boolean = true,
    val useCustomMaddookieArt: Boolean = true,
    val statusHeadline: String = "Streaming on Maddookie",
    val activeAnimeTitle: String = "",
    val activeEpisode: Int = 0,
    val activeTotalEpisodes: Int? = null,
    val activeProvider: String = "",
    val activeAudioMode: String = "SUB",
    val lastSyncedTimestamp: Long = 0L
)

data class MemorySnapshot(
    val appUsedHeapMb: Long,
    val appMaxHeapMb: Long,
    val deviceAvailableMb: Long,
    val deviceTotalMb: Long,
    val isLowRamDevice: Boolean
)
