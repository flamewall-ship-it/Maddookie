package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.WatchStatus
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "watchlist_entries")
data class WatchlistEntity(
    @PrimaryKey val animeId: Int,
    val malId: Int? = null,
    val title: String,
    val coverUrl: String,
    val bannerUrl: String? = null,
    val totalEpisodes: Int? = null,
    val watchedEpisodes: Int = 0,
    val watchStatus: String = WatchStatus.WATCHING.name,
    val averageScore: Int? = null,
    val lastWatchedEpisode: Int = 1,
    val lastPositionMs: Long = 0L,
    val lastDurationMs: Long = 0L,
    val lastAudioMode: String = "SUB",
    val lastProvider: String = "ALLANIME",
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    fun statusEnum(): WatchStatus =
        WatchStatus.entries.firstOrNull { it.name == watchStatus } ?: WatchStatus.WATCHING
}
