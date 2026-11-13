package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.Coil
import com.example.MaddookieApp
import com.example.data.local.WatchlistEntity
import com.example.data.model.AiringEntry
import com.example.data.model.AnimeMedia
import com.example.data.model.DiscordRpcConfig
import com.example.data.model.MemorySnapshot
import com.example.data.model.PotatoTurboConfig
import com.example.data.model.ResolvedStream
import com.example.data.model.StreamAudioMode
import com.example.data.model.StreamProvider
import com.example.data.model.StreamResolutionAttempt
import com.example.data.model.SyncAccountState
import com.example.data.model.WatchStatus
import com.example.util.MemoryTelemetry
import com.example.util.PotatoTurboCoilFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiLoadState<out T> {
    data object Loading : UiLoadState<Nothing>
    data class Success<T>(val data: T) : UiLoadState<T>
    data class Error(val message: String) : UiLoadState<Nothing>
}

data class DiscoverUiState(
    val trending: List<AnimeMedia> = emptyList(),
    val seasonal: List<AnimeMedia> = emptyList(),
    val topRated: List<AnimeMedia> = emptyList(),
    val searchQuery: String = "",
    val selectedGenre: String = "All",
    val searchResults: List<AnimeMedia> = emptyList(),
    val isSearching: Boolean = false,
    val loadState: UiLoadState<Unit> = UiLoadState.Loading
)

data class PlayerUiState(
    val anime: AnimeMedia? = null,
    val episode: Int = 1,
    val audioMode: StreamAudioMode = StreamAudioMode.SUB,
    val preferredProvider: StreamProvider? = null,
    val isResolving: Boolean = false,
    val attempts: List<StreamResolutionAttempt> = emptyList(),
    val resolvedStream: ResolvedStream? = null,
    val errorMessage: String? = null,
    val synced85ForCurrentEpisode: Boolean = false,
    val syncStatusBanner: String? = null
)

class MaddookieViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MaddookieApp
    private val aniList = app.aniListClient
    private val authManager = app.trackerAuthManager
    private val streamingEngine = app.streamingEngine
    private val watchlistRepo = app.watchlistRepository
    private val prefs = app.preferences

    private val mediaCache = LinkedHashMap<Int, AnimeMedia>()

    private val _discoverState = MutableStateFlow(DiscoverUiState())
    val discoverState: StateFlow<DiscoverUiState> = _discoverState.asStateFlow()

    private val _scheduleState = MutableStateFlow<UiLoadState<List<AiringEntry>>>(UiLoadState.Loading)
    val scheduleState: StateFlow<UiLoadState<List<AiringEntry>>> = _scheduleState.asStateFlow()

    private val _detailState = MutableStateFlow<UiLoadState<AnimeMedia>>(UiLoadState.Loading)
    val detailState: StateFlow<UiLoadState<AnimeMedia>> = _detailState.asStateFlow()

    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

    private val _authBannerMessage = MutableStateFlow<String?>(null)
    val authBannerMessage: StateFlow<String?> = _authBannerMessage.asStateFlow()

    private val _memorySnapshot = MutableStateFlow(MemoryTelemetry.capture(application))
    val memorySnapshot: StateFlow<MemorySnapshot> = _memorySnapshot.asStateFlow()

    val watchlistEntries: StateFlow<List<WatchlistEntity>> = watchlistRepo.allEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val turboConfig: StateFlow<PotatoTurboConfig> = prefs.turboConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PotatoTurboConfig())

    val syncState: StateFlow<SyncAccountState> = prefs.syncState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncAccountState())

    val discordRpc: StateFlow<DiscordRpcConfig> = prefs.discordRpcConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiscordRpcConfig())

    init {
        loadDiscover()
        loadAiringSchedule()
    }

    fun loadDiscover() {
        viewModelScope.launch {
            _discoverState.update { it.copy(loadState = UiLoadState.Loading) }
            aniList.fetchDiscoverCatalog()
                .onSuccess { catalog ->
                    (catalog.trending + catalog.seasonal + catalog.topRated).forEach {
                        mediaCache[it.id] = it
                    }
                    _discoverState.update {
                        it.copy(
                            trending = catalog.trending,
                            seasonal = catalog.seasonal,
                            topRated = catalog.topRated,
                            loadState = UiLoadState.Success(Unit)
                        )
                    }
                }
                .onFailure { err ->
                    _discoverState.update {
                        it.copy(loadState = UiLoadState.Error(err.message ?: "Network error loading AniList"))
                    }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _discoverState.update { it.copy(searchQuery = query) }
        if (query.isBlank() && _discoverState.value.selectedGenre == "All") {
            _discoverState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        executeSearch(query, _discoverState.value.selectedGenre)
    }

    fun onGenreSelected(genre: String) {
        _discoverState.update { it.copy(selectedGenre = genre) }
        if (genre == "All" && _discoverState.value.searchQuery.isBlank()) {
            _discoverState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        executeSearch(_discoverState.value.searchQuery, genre)
    }

    private fun executeSearch(query: String, genre: String) {
        viewModelScope.launch {
            _discoverState.update { it.copy(isSearching = true) }
            aniList.searchAnime(query, genre)
                .onSuccess { results ->
                    results.forEach { mediaCache[it.id] = it }
                    _discoverState.update { it.copy(searchResults = results, isSearching = false) }
                }
                .onFailure {
                    _discoverState.update { it.copy(isSearching = false) }
                }
        }
    }

    fun loadAiringSchedule() {
        viewModelScope.launch {
            _scheduleState.value = UiLoadState.Loading
            val nowSec = System.currentTimeMillis() / 1000L
            val sevenDaysLater = nowSec + 7L * 24L * 3600L
            aniList.fetchAiringSchedule(nowSec - 3600L, sevenDaysLater)
                .onSuccess { entries ->
                    entries.forEach { mediaCache[it.media.id] = it.media }
                    _scheduleState.value = UiLoadState.Success(entries)
                }
                .onFailure { err ->
                    _scheduleState.value = UiLoadState.Error(err.message ?: "Failed to load airing schedule")
                }
        }
    }

    fun loadAnimeDetail(animeId: Int) {
        val cached = mediaCache[animeId]
        if (cached != null) {
            _detailState.value = UiLoadState.Success(cached)
        } else {
            _detailState.value = UiLoadState.Loading
        }
        viewModelScope.launch {
            aniList.fetchAnimeDetails(animeId)
                .onSuccess { fresh ->
                    mediaCache[fresh.id] = fresh
                    _detailState.value = UiLoadState.Success(fresh)
                }
                .onFailure { err ->
                    if (cached == null) {
                        _detailState.value = UiLoadState.Error(err.message ?: "Unable to fetch anime details")
                    }
                }
        }
    }

    fun updateWatchStatus(media: AnimeMedia, status: WatchStatus) {
        viewModelScope.launch {
            val updated = watchlistRepo.setWatchStatus(media, status)
            syncProgressToTrackers(media, updated.watchedEpisodes, status)
        }
    }

    fun incrementWatchlistEntry(entry: WatchlistEntity) {
        viewModelScope.launch {
            val updated = watchlistRepo.incrementEpisode(entry)
            val media = mediaCache[entry.animeId] ?: AnimeMedia(
                id = entry.animeId,
                malId = entry.malId,
                titleRomaji = entry.title,
                coverUrl = entry.coverUrl,
                episodes = entry.totalEpisodes
            )
            syncProgressToTrackers(media, updated.watchedEpisodes, updated.statusEnum())
        }
    }

    fun removeFromWatchlist(animeId: Int) {
        viewModelScope.launch { watchlistRepo.remove(animeId) }
    }

    fun startEpisodePlayback(
        animeId: Int,
        episode: Int,
        audioMode: StreamAudioMode,
        preferredProvider: StreamProvider? = null
    ) {
        viewModelScope.launch {
            val media = mediaCache[animeId] ?: aniList.fetchAnimeDetails(animeId).getOrNull()
            if (media == null) {
                _playerState.update {
                    it.copy(
                        isResolving = false,
                        errorMessage = "Could not load anime metadata #$animeId from AniList."
                    )
                }
                return@launch
            }
            mediaCache[media.id] = media

            _playerState.value = PlayerUiState(
                anime = media,
                episode = episode,
                audioMode = audioMode,
                preferredProvider = preferredProvider,
                isResolving = true,
                attempts = emptyList(),
                resolvedStream = null,
                errorMessage = null,
                synced85ForCurrentEpisode = false
            )

            // Update Discord Rich Presence immediately
            updateDiscordPresence(media, episode, preferredProvider?.displayName ?: "Multi-Provider", audioMode)

            val result = streamingEngine.resolveEpisodeStream(
                anime = media,
                episode = episode,
                audioMode = audioMode,
                preferredProvider = preferredProvider,
                onAttemptUpdate = { attempts ->
                    _playerState.update { it.copy(attempts = attempts) }
                }
            )

            result.onSuccess { stream ->
                updateDiscordPresence(media, episode, stream.provider.displayName, audioMode)
                _playerState.update {
                    it.copy(
                        isResolving = false,
                        resolvedStream = stream,
                        attempts = stream.attempts,
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _playerState.update {
                    it.copy(
                        isResolving = false,
                        errorMessage = err.message ?: "Streaming providers unreachable"
                    )
                }
            }
        }
    }

    fun switchPlayerAudioMode(audioMode: StreamAudioMode) {
        val current = _playerState.value
        val anime = current.anime ?: return
        startEpisodePlayback(anime.id, current.episode, audioMode, current.preferredProvider)
    }

    fun switchPlayerProvider(provider: StreamProvider) {
        val current = _playerState.value
        val anime = current.anime ?: return
        startEpisodePlayback(anime.id, current.episode, current.audioMode, provider)
    }

    fun changePlayerEpisode(episode: Int) {
        val current = _playerState.value
        val anime = current.anime ?: return
        val clamped = episode.coerceAtLeast(1)
        startEpisodePlayback(anime.id, clamped, current.audioMode, current.preferredProvider)
    }

    fun onPlaybackProgressTick(positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L) return
        val state = _playerState.value
        val anime = state.anime ?: return
        val ratio = positionMs.toFloat() / durationMs.toFloat()
        val crossed85 = ratio >= 0.85f && !state.synced85ForCurrentEpisode

        if (crossed85) {
            _playerState.update { it.copy(synced85ForCurrentEpisode = true) }
            viewModelScope.launch {
                val syncedServices = syncProgressToTrackers(anime, state.episode, WatchStatus.WATCHING)
                watchlistRepo.recordPlaybackProgress(
                    media = anime,
                    episodeNumber = state.episode,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    audioMode = state.audioMode.label,
                    providerName = state.resolvedStream?.provider?.name ?: "ALLANIME",
                    crossed85Percent = true,
                    syncedAniList = syncedServices.first,
                    syncedMal = syncedServices.second,
                    syncedSimkl = syncedServices.third
                )
                _playerState.update {
                    it.copy(syncStatusBanner = "85% watched • Synced Ep ${state.episode} progress")
                }
            }
        }
    }

    private suspend fun syncProgressToTrackers(
        media: AnimeMedia,
        episode: Int,
        status: WatchStatus
    ): Triple<Boolean, Boolean, Boolean> {
        val currentSync = syncState.value
        if (!currentSync.autoSyncAt85Percent) return Triple(false, false, false)

        var alOk = false
        var malOk = false
        var simklOk = false

        if (currentSync.anilistConnected && currentSync.anilistToken.isNotBlank()) {
            alOk = aniList.saveMediaListProgress(
                currentSync.anilistToken,
                media.id,
                episode,
                status
            ).isSuccess
        }
        if (currentSync.malConnected && currentSync.malToken.isNotBlank() && media.malId != null) {
            malOk = authManager.syncMalProgress(
                currentSync.malToken,
                media.malId,
                episode,
                status
            ).isSuccess
        }
        if (currentSync.simklConnected && currentSync.simklToken.isNotBlank() && currentSync.simklClientId.isNotBlank()) {
            simklOk = authManager.syncSimklHistory(
                currentSync.simklClientId,
                currentSync.simklToken,
                media.malId,
                episode
            ).isSuccess
        }
        return Triple(alOk, malOk, simklOk)
    }

    private fun updateDiscordPresence(
        media: AnimeMedia,
        episode: Int,
        providerName: String,
        audioMode: StreamAudioMode
    ) {
        viewModelScope.launch {
            prefs.updateDiscordRpc { cfg ->
                cfg.copy(
                    activeAnimeTitle = media.displayTitle,
                    activeEpisode = episode,
                    activeTotalEpisodes = media.episodes,
                    activeProvider = providerName,
                    activeAudioMode = audioMode.label,
                    lastSyncedTimestamp = System.currentTimeMillis()
                )
            }
        }
    }

    // --- Potato-Turbo Settings ---
    fun updateTurboSettings(transform: (PotatoTurboConfig) -> PotatoTurboConfig) {
        viewModelScope.launch {
            prefs.updateTurboConfig(transform)
            val latest = prefs.turboConfig.first()
            Coil.setImageLoader(
                PotatoTurboCoilFactory.build(app, app.okHttpClient, latest)
            )
            refreshMemoryTelemetry()
        }
    }

    fun refreshMemoryTelemetry() {
        _memorySnapshot.value = MemoryTelemetry.capture(app)
    }

    // --- Two-Way AniList, MAL OAuth2 PKCE & Simkl Device PIN ---
    fun saveOAuthClientIds(anilistId: String, malId: String, simklId: String) {
        viewModelScope.launch {
            prefs.saveClientIds(anilistId, malId, simklId)
            _authBannerMessage.value = "OAuth Client IDs saved securely."
        }
    }

    fun startAniListOAuth(context: Context) {
        val clientId = syncState.value.anilistClientId
        if (clientId.isBlank()) {
            _authBannerMessage.value = "Enter your AniList OAuth Client ID first (redirect: maddookie://auth)."
            return
        }
        val verifier = authManager.generatePkceVerifier()
        viewModelScope.launch {
            prefs.savePkceVerifier("anilist", verifier)
            authManager.launchAniListOAuth(context, clientId, verifier)
        }
    }

    fun connectAniListWithToken(token: String) {
        viewModelScope.launch {
            authManager.verifyManualAniListToken(token)
                .onSuccess { username ->
                    prefs.setAniListSession(true, username, token.trim())
                    _authBannerMessage.value = "Connected AniList as $username"
                }
                .onFailure { err ->
                    _authBannerMessage.value = "AniList auth error: ${err.message}"
                }
        }
    }

    fun startMalOAuth(context: Context) {
        val clientId = syncState.value.malClientId
        if (clientId.isBlank()) {
            _authBannerMessage.value = "Enter your MyAnimeList Client ID first (redirect: maddookie://mal-auth)."
            return
        }
        val verifier = authManager.generatePkceVerifier()
        viewModelScope.launch {
            prefs.savePkceVerifier("mal", verifier)
            authManager.launchMalOAuth(context, clientId, verifier)
        }
    }

    fun handleOAuthDeepLink(uri: Uri) {
        viewModelScope.launch {
            when (uri.host) {
                "auth" -> {
                    val tokenFromFragment = uri.fragment
                        ?.split("&")
                        ?.firstOrNull { it.startsWith("access_token=") }
                        ?.substringAfter("access_token=")
                    val code = uri.getQueryParameter("code")
                    if (!tokenFromFragment.isNullOrBlank()) {
                        connectAniListWithToken(tokenFromFragment)
                    } else if (!code.isNullOrBlank()) {
                        _authBannerMessage.value = "AniList authorization code received (maddookie://auth). Paste token or exchange via server."
                    }
                }
                "mal-auth" -> {
                    val code = uri.getQueryParameter("code")
                    val clientId = syncState.value.malClientId
                    if (!code.isNullOrBlank() && clientId.isNotBlank()) {
                        val verifier = prefs.getPkceVerifier("mal")
                        authManager.exchangeMalCode(clientId, code, verifier)
                            .onSuccess { (username, token) ->
                                prefs.setMalSession(true, username, token)
                                _authBannerMessage.value = "Connected MyAnimeList as $username"
                            }
                            .onFailure { err ->
                                _authBannerMessage.value = "MAL PKCE exchange failed: ${err.message}"
                            }
                    }
                }
            }
        }
    }

    fun requestSimklDevicePin() {
        val clientId = syncState.value.simklClientId
        if (clientId.isBlank()) {
            _authBannerMessage.value = "Enter your Simkl Client ID to generate a simkl.com/pin code."
            return
        }
        viewModelScope.launch {
            authManager.requestSimklPin(clientId)
                .onSuccess { challenge ->
                    val expiresAt = System.currentTimeMillis() + challenge.expiresInSeconds * 1000L
                    prefs.setSimklPinState(challenge.userCode, expiresAt)
                    _authBannerMessage.value = "Enter PIN ${challenge.userCode} at simkl.com/pin"
                }
                .onFailure { err ->
                    _authBannerMessage.value = "Simkl PIN error: ${err.message}"
                }
        }
    }

    fun verifySimklPinNow() {
        val state = syncState.value
        if (state.simklClientId.isBlank() || state.simklUserCode.isBlank()) return
        viewModelScope.launch {
            authManager.pollSimklPin(state.simklClientId, state.simklUserCode)
                .onSuccess { token ->
                    if (!token.isNullOrBlank()) {
                        prefs.setSimklSession(true, token)
                        _authBannerMessage.value = "Simkl Device PIN authorized!"
                    } else {
                        _authBannerMessage.value = "PIN ${state.simklUserCode} still awaiting authorization at simkl.com/pin."
                    }
                }
                .onFailure { err ->
                    _authBannerMessage.value = "Simkl poll error: ${err.message}"
                }
        }
    }

    fun disconnectTracker(service: String) {
        viewModelScope.launch {
            when (service) {
                "anilist" -> prefs.setAniListSession(false, "", "")
                "mal" -> prefs.setMalSession(false, "", "")
                "simkl" -> prefs.setSimklSession(false, "")
            }
        }
    }

    fun setAutoSync85(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoSync85(enabled) }
    }

    fun updateDiscordRpc(transform: (DiscordRpcConfig) -> DiscordRpcConfig) {
        viewModelScope.launch { prefs.updateDiscordRpc(transform) }
    }

    fun clearAuthBanner() {
        _authBannerMessage.value = null
    }
}
