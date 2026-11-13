package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object DiscoverRoute

@Serializable
data object ScheduleRoute

@Serializable
data object WatchlistRoute

@Serializable
data object SyncRpcRoute

@Serializable
data object PotatoTurboRoute

@Serializable
data class AnimeDetailRoute(val animeId: Int)

@Serializable
data class PlayerRoute(
    val animeId: Int,
    val episode: Int,
    val audioMode: String = "SUB"
)
