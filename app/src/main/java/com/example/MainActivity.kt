package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.model.StreamAudioMode
import com.example.ui.navigation.AnimeDetailRoute
import com.example.ui.navigation.DiscoverRoute
import com.example.ui.navigation.PlayerRoute
import com.example.ui.navigation.PotatoTurboRoute
import com.example.ui.navigation.ScheduleRoute
import com.example.ui.navigation.SyncRpcRoute
import com.example.ui.navigation.WatchlistRoute
import com.example.ui.screens.AnimeDetailScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.PotatoTurboScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.SyncRpcScreen
import com.example.ui.screens.WatchlistScreen
import com.example.ui.theme.MaddookieTheme
import com.example.ui.viewmodel.MaddookieViewModel

private data class TopLevelDestination(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {

    private val viewModel: MaddookieViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val turboConfig by viewModel.turboConfig.collectAsStateWithLifecycle()

            MaddookieTheme(
                darkTheme = turboConfig.darkTheme,
                zeroBlurFastPath = turboConfig.zeroBlurFastPath
            ) {
                MaddookieShell(
                    viewModel = viewModel,
                    useNavigationRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                )
            }
        }
    }
}

@Composable
private fun MaddookieShell(
    viewModel: MaddookieViewModel,
    useNavigationRail: Boolean
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    val context = LocalContext.current

    val discoverState by viewModel.discoverState.collectAsStateWithLifecycle()
    val scheduleState by viewModel.scheduleState.collectAsStateWithLifecycle()
    val watchlistEntries by viewModel.watchlistEntries.collectAsStateWithLifecycle()
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val trackingSettings by viewModel.trackingSettings.collectAsStateWithLifecycle()
    val discordRpc by viewModel.discordRpc.collectAsStateWithLifecycle()
    val turboConfig by viewModel.turboConfig.collectAsStateWithLifecycle()
    val memorySnapshot by viewModel.memorySnapshot.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportWatchlistJson(uri, context.contentResolver)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importWatchlistJson(uri, context.contentResolver)
        }
    }

    val destinations = listOf(
        TopLevelDestination(
            route = DiscoverRoute,
            labelRes = R.string.nav_discover,
            selectedIcon = Icons.Filled.Explore,
            unselectedIcon = Icons.Outlined.Explore,
            testTag = "nav_discover"
        ),
        TopLevelDestination(
            route = ScheduleRoute,
            labelRes = R.string.nav_schedule,
            selectedIcon = Icons.Filled.CalendarMonth,
            unselectedIcon = Icons.Outlined.CalendarMonth,
            testTag = "nav_schedule"
        ),
        TopLevelDestination(
            route = WatchlistRoute,
            labelRes = R.string.nav_watchlist,
            selectedIcon = Icons.Filled.Bookmarks,
            unselectedIcon = Icons.Outlined.Bookmarks,
            testTag = "nav_watchlist"
        ),
        TopLevelDestination(
            route = SyncRpcRoute,
            labelRes = R.string.nav_sync,
            selectedIcon = Icons.Filled.Tune,
            unselectedIcon = Icons.Outlined.Tune,
            testTag = "nav_sync"
        ),
        TopLevelDestination(
            route = PotatoTurboRoute,
            labelRes = R.string.nav_turbo,
            selectedIcon = Icons.Filled.Bolt,
            unselectedIcon = Icons.Outlined.Bolt,
            testTag = "nav_turbo"
        )
    )

    val isTopLevel = currentDest?.let { dest ->
        dest.hasRoute<DiscoverRoute>() ||
            dest.hasRoute<ScheduleRoute>() ||
            dest.hasRoute<WatchlistRoute>() ||
            dest.hasRoute<SyncRpcRoute>() ||
            dest.hasRoute<PotatoTurboRoute>()
    } ?: true

    val navigateTopLevel: (Any) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(DiscoverRoute) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!useNavigationRail && isTopLevel) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    destinations.forEach { dest ->
                        val selected = when (dest.route) {
                            is DiscoverRoute -> currentDest?.hasRoute<DiscoverRoute>() == true
                            is ScheduleRoute -> currentDest?.hasRoute<ScheduleRoute>() == true
                            is WatchlistRoute -> currentDest?.hasRoute<WatchlistRoute>() == true
                            is SyncRpcRoute -> currentDest?.hasRoute<SyncRpcRoute>() == true
                            is PotatoTurboRoute -> currentDest?.hasRoute<PotatoTurboRoute>() == true
                            else -> false
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateTopLevel(dest.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = stringResource(dest.labelRes)
                                )
                            },
                            label = { Text(stringResource(dest.labelRes), maxLines = 1) },
                            modifier = Modifier.testTag(dest.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (useNavigationRail && isTopLevel) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    destinations.forEach { dest ->
                        val selected = when (dest.route) {
                            is DiscoverRoute -> currentDest?.hasRoute<DiscoverRoute>() == true
                            is ScheduleRoute -> currentDest?.hasRoute<ScheduleRoute>() == true
                            is WatchlistRoute -> currentDest?.hasRoute<WatchlistRoute>() == true
                            is SyncRpcRoute -> currentDest?.hasRoute<SyncRpcRoute>() == true
                            is PotatoTurboRoute -> currentDest?.hasRoute<PotatoTurboRoute>() == true
                            else -> false
                        }
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTopLevel(dest.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = stringResource(dest.labelRes)
                                )
                            },
                            label = { Text(stringResource(dest.labelRes)) },
                            modifier = Modifier.testTag(dest.testTag)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                NavHost(
                    navController = navController,
                    startDestination = DiscoverRoute,
                    modifier = Modifier.widthIn(max = 1100.dp)
                ) {
                    composable<DiscoverRoute> {
                        DiscoverScreen(
                            state = discoverState,
                            continueWatching = watchlistEntries.take(8),
                            turboEnabled = turboConfig.rgb565Enabled || turboConfig.zeroBlurFastPath,
                            onSearchQueryChange = viewModel::onSearchQueryChanged,
                            onGenreSelect = viewModel::onGenreSelected,
                            onRetry = viewModel::loadDiscover,
                            onOpenAnime = { animeId ->
                                navController.navigate(AnimeDetailRoute(animeId))
                            },
                            onQuickPlay = { animeId, ep ->
                                navController.navigate(PlayerRoute(animeId, ep, StreamAudioMode.SUB.name))
                            }
                        )
                    }

                    composable<ScheduleRoute> {
                        ScheduleScreen(
                            state = scheduleState,
                            onRefresh = viewModel::loadAiringSchedule,
                            onOpenAnime = { animeId ->
                                navController.navigate(AnimeDetailRoute(animeId))
                            }
                        )
                    }

                    composable<WatchlistRoute> {
                        WatchlistScreen(
                            entries = watchlistEntries,
                            onOpenAnime = { animeId ->
                                navController.navigate(AnimeDetailRoute(animeId))
                            },
                            onPlayNext = { animeId, ep ->
                                navController.navigate(PlayerRoute(animeId, ep, StreamAudioMode.SUB.name))
                            },
                            onIncrementEpisode = viewModel::incrementWatchlistEntry,
                            onRemove = viewModel::removeFromWatchlist
                        )
                    }

                    composable<SyncRpcRoute> {
                        SyncRpcScreen(
                            trackingSettings = trackingSettings,
                            watchlistEntries = watchlistEntries,
                            discordRpc = discordRpc,
                            statusBanner = statusBanner,
                            onToggleAutoTrack85 = viewModel::setAutoTrack85,
                            onExportJsonClick = {
                                exportLauncher.launch("maddookie_watchlist_backup.json")
                            },
                            onImportJsonClick = {
                                importLauncher.launch(arrayOf("application/json", "text/plain"))
                            },
                            onUpdateDiscordRpc = viewModel::updateDiscordRpc,
                            onDismissBanner = viewModel::clearStatusBanner
                        )
                    }

                    composable<PotatoTurboRoute> {
                        LaunchedEffect(Unit) { viewModel.refreshMemoryTelemetry() }
                        PotatoTurboScreen(
                            config = turboConfig,
                            memory = memorySnapshot,
                            onUpdateConfig = viewModel::updateTurboSettings,
                            onRefreshTelemetry = viewModel::refreshMemoryTelemetry
                        )
                    }

                    composable<AnimeDetailRoute> { entry ->
                        val route = entry.toRoute<AnimeDetailRoute>()
                        LaunchedEffect(route.animeId) {
                            viewModel.loadAnimeDetail(route.animeId)
                        }
                        val watchlistMatch = watchlistEntries.firstOrNull { it.animeId == route.animeId }
                        AnimeDetailScreen(
                            detailState = detailState,
                            watchlistEntry = watchlistMatch,
                            onBack = { navController.popBackStack() },
                            onRetry = { viewModel.loadAnimeDetail(route.animeId) },
                            onUpdateStatus = viewModel::updateWatchStatus,
                            onPlayEpisode = { id, ep, audio ->
                                navController.navigate(PlayerRoute(id, ep, audio.name))
                            }
                        )
                    }

                    composable<PlayerRoute> { entry ->
                        val route = entry.toRoute<PlayerRoute>()
                        val initialAudio = StreamAudioMode.entries.firstOrNull { it.name == route.audioMode }
                            ?: StreamAudioMode.SUB
                        LaunchedEffect(route.animeId, route.episode, route.audioMode) {
                            viewModel.startEpisodePlayback(route.animeId, route.episode, initialAudio)
                        }
                        PlayerScreen(
                            state = playerState,
                            onBack = { navController.popBackStack() },
                            onSwitchAudio = viewModel::switchPlayerAudioMode,
                            onSwitchProvider = viewModel::switchPlayerProvider,
                            onChangeEpisode = viewModel::changePlayerEpisode,
                            onRetry = {
                                viewModel.startEpisodePlayback(
                                    route.animeId,
                                    playerState.episode,
                                    playerState.audioMode,
                                    playerState.preferredProvider
                                )
                            },
                            onProgressTick = viewModel::onPlaybackProgressTick
                        )
                    }
                }
            }
        }
    }
}
