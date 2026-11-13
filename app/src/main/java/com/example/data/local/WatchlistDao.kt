package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist_entries ORDER BY updatedAtMs DESC")
    fun observeAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist_entries WHERE animeId = :animeId LIMIT 1")
    fun observeById(animeId: Int): Flow<WatchlistEntity?>

    @Query("SELECT * FROM watchlist_entries WHERE animeId = :animeId LIMIT 1")
    suspend fun getById(animeId: Int): WatchlistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WatchlistEntity)

    @Query("DELETE FROM watchlist_entries WHERE animeId = :animeId")
    suspend fun deleteById(animeId: Int)
}
