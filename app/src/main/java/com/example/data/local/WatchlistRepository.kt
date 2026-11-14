package com.example.data.local

import com.example.data.model.AnimeMedia
import com.example.data.model.WatchStatus
import kotlinx.coroutines.flow.Flow

class WatchlistRepository(private val dao: WatchlistDao) {

    val allEntries: Flow<List<WatchlistEntity>> = dao.observeAll()

    fun observeEntry(animeId: Int): Flow<WatchlistEntity?> = dao.observeById(animeId)

    suspend fun getEntry(animeId: Int): WatchlistEntity? = dao.getById(animeId)

    suspend fun setWatchStatus(
        media: AnimeMedia,
        status: WatchStatus,
        watchedEpisodes: Int? = null
    ): WatchlistEntity {
        val existing = dao.getById(media.id)
        val total = media.episodes
        val nextWatched = when {
            watchedEpisodes != null -> watchedEpisodes
            status == WatchStatus.COMPLETED && total != null -> total
            existing != null -> existing.watchedEpisodes
            else -> 0
        }
        val entity = (existing ?: WatchlistEntity(
            animeId = media.id,
            malId = media.malId,
            title = media.displayTitle,
            coverUrl = media.coverUrl,
            bannerUrl = media.bannerUrl,
            totalEpisodes = total,
            averageScore = media.averageScore
        )).copy(
            malId = media.malId ?: existing?.malId,
            title = media.displayTitle,
            coverUrl = media.coverUrl,
            bannerUrl = media.bannerUrl ?: existing?.bannerUrl,
            totalEpisodes = total ?: existing?.totalEpisodes,
            averageScore = media.averageScore ?: existing?.averageScore,
            watchStatus = status.name,
            watchedEpisodes = nextWatched.coerceAtLeast(0),
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsert(entity)
        return entity
    }

    suspend fun recordPlaybackProgress(
        media: AnimeMedia,
        episodeNumber: Int,
        positionMs: Long,
        durationMs: Long,
        audioMode: String,
        providerName: String,
        crossed85Percent: Boolean
    ): WatchlistEntity {
        val existing = dao.getById(media.id)
        val currentWatched = existing?.watchedEpisodes ?: 0
        val newWatched = if (crossed85Percent) {
            maxOf(currentWatched, episodeNumber)
        } else {
            currentWatched
        }
        val total = media.episodes ?: existing?.totalEpisodes
        val resolvedStatus = when {
            total != null && newWatched >= total -> WatchStatus.COMPLETED
            existing != null -> existing.statusEnum()
            else -> WatchStatus.WATCHING
        }
        val entity = (existing ?: WatchlistEntity(
            animeId = media.id,
            malId = media.malId,
            title = media.displayTitle,
            coverUrl = media.coverUrl,
            bannerUrl = media.bannerUrl,
            totalEpisodes = total,
            averageScore = media.averageScore
        )).copy(
            watchedEpisodes = newWatched,
            watchStatus = resolvedStatus.name,
            lastWatchedEpisode = episodeNumber,
            lastPositionMs = positionMs,
            lastDurationMs = durationMs,
            lastAudioMode = audioMode,
            lastProvider = providerName,
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsert(entity)
        return entity
    }

    suspend fun incrementEpisode(entry: WatchlistEntity): WatchlistEntity {
        val total = entry.totalEpisodes
        val nextWatched = if (total != null) {
            (entry.watchedEpisodes + 1).coerceAtMost(total)
        } else {
            entry.watchedEpisodes + 1
        }
        val nextStatus = if (total != null && nextWatched >= total) {
            WatchStatus.COMPLETED
        } else {
            entry.statusEnum()
        }
        val updated = entry.copy(
            watchedEpisodes = nextWatched,
            lastWatchedEpisode = maxOf(entry.lastWatchedEpisode, nextWatched),
            watchStatus = nextStatus.name,
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsert(updated)
        return updated
    }

    suspend fun importAll(entries: List<WatchlistEntity>) {
        dao.upsertAll(entries)
    }

    suspend fun remove(animeId: Int) {
        dao.deleteById(animeId)
    }
}
