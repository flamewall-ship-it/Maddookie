package com.example.ui.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.Coil
import com.example.MaddookieApp
import com.example.data.local.WatchlistEntity
import com.example.data.model.AiringEntry
import com.example.data.model.AnimeMedia
import com.example.data.model.DiscordRpcConfig
import com.example.data.model.LocalTrackingSettings
import com.example.data.model.MemorySnapshot
import com.example.data.model.PotatoTurboConfig
import com.example.data.model.ResolvedStream
import com.example.data.model.StreamAudioMode
import com.example.data.model.StreamProvider
import com.example.data.model.StreamResolutionAttempt
import com.example.data.model.WatchStatus
import com.example.util.MemoryTelemetry
import com.example.util.PotatoTurboCoilFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
    private val streamingEngine = app.streamingEngine
    private val watchlistRepo = app.watchlistRepository
    private val prefs = app.preferences

    private val backupJson = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val mediaCache = LinkedHashMap<Int, AnimeMedia>()

    private val _discoverState = MutableStateFlow(DiscoverUiState())
    val discoverState: StateFlow<DiscoverUiState> = _discoverState.asStateFlow()

    private val _scheduleState = MutableStateFlow<UiLoadState<List<AiringEntry>>>(UiLoadState.Loading)
    val scheduleState: StateFlow<UiLoadState<List<AiringEntry>>> = _scheduleState.asStateFlow()

    private val _detailState = MutableStateFlow<UiLoadState<AnimeMedia>>(UiLoadState.Loading)
    val detailState: StateFlow<UiLoadState<AnimeMedia>> = _detailState.asStateFlow()

    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _memorySnapshot = MutableStateFlow(MemoryTelemetry.capture(application))
    val memorySnapshot: StateFlow<MemorySnapshot> = _memorySnapshot.asStateFlow()

    val watchlistEntries: StateFlow<List<WatchlistEntity>> = watchlistRepo.allEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val turboConfig: StateFlow<PotatoTurboConfig> = prefs.turboConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PotatoTurboConfig())

    val trackingSettings: StateFlow<LocalTrackingSettings> = prefs.localTrackingSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalTrackingSettings())

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
                        it.copy(loadState = UiLoadState.Error(err.message ?: "Network error loading catalog"))
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
            watchlistRepo.setWatchStatus(media, status)
        }
    }

    fun incrementWatchlistEntry(entry: WatchlistEntity) {
        viewModelScope.launch {
            watchlistRepo.incrementEpisode(entry)
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
                        errorMessage = "Could not load anime metadata #$animeId."
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
        val autoTrack = trackingSettings.value.autoTrackAt85Percent
        val ratio = positionMs.toFloat() / durationMs.toFloat()
        val crossed85 = autoTrack && ratio >= 0.85f && !state.synced85ForCurrentEpisode

        if (crossed85) {
            _playerState.update { it.copy(synced85ForCurrentEpisode = true) }
            viewModelScope.launch {
                watchlistRepo.recordPlaybackProgress(
                    media = anime,
                    episodeNumber = state.episode,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    audioMode = state.audioMode.label,
                    providerName = state.resolvedStream?.provider?.name ?: "ALLANIME",
                    crossed85Percent = true
                )
                _playerState.update {
                    it.copy(syncStatusBanner = "85% watched • Saved Ep ${state.episode} locally")
                }
            }
        }
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

    fun setAutoTrack85(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoTrack85(enabled) }
    }

    fun exportWatchlistJson(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            runCatching {
                val snapshot = watchlistEntries.value
                val encoded = backupJson.encodeToString(snapshot)
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(encoded.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open destination file")
                }
                prefs.setLastBackupAt(System.currentTimeMillis())
                snapshot.size
            }.onSuccess { count ->
                _statusBannerMessage.value = "Exported $count watchlist entries to JSON backup."
            }.onFailure { err ->
                _statusBannerMessage.value = "Export failed: ${err.message}"
            }
        }
    }

    fun importWatchlistJson(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            runCatching {
                val raw = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    } ?: error("Unable to read backup file")
                }
                val parsed = backupJson.decodeFromString<List<WatchlistEntity>>(raw)
                watchlistRepo.importAll(parsed)
                parsed.size
            }.onSuccess { count ->
                _statusBannerMessage.value = "Imported $count entries into local Room watchlist."
            }.onFailure { err ->
                _statusBannerMessage.value = "Import failed: ${err.message}"
            }
        }
    }

    fun updateDiscordRpc(transform: (DiscordRpcConfig) -> DiscordRpcConfig) {
        viewModelScope.launch { prefs.updateDiscordRpc(transform) }
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }
}
