package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.WatchlistEntity
import com.example.data.model.DiscordRpcConfig
import com.example.data.model.LocalTrackingSettings
import com.example.data.model.WatchStatus
import com.example.ui.theme.DiscordBlurple
import com.example.ui.theme.SimklEmerald
import com.example.ui.theme.TurboLime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncRpcScreen(
    trackingSettings: LocalTrackingSettings,
    watchlistEntries: List<WatchlistEntity>,
    discordRpc: DiscordRpcConfig,
    statusBanner: String?,
    onToggleAutoTrack85: (Boolean) -> Unit,
    onExportJsonClick: () -> Unit,
    onImportJsonClick: () -> Unit,
    onUpdateDiscordRpc: ((DiscordRpcConfig) -> DiscordRpcConfig) -> Unit,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val watchingCount = remember(watchlistEntries) {
        watchlistEntries.count { it.statusEnum() == WatchStatus.WATCHING }
    }
    val completedCount = remember(watchlistEntries) {
        watchlistEntries.count { it.statusEnum() == WatchStatus.COMPLETED }
    }
    val totalEpisodes = remember(watchlistEntries) {
        watchlistEntries.sumOf { it.watchedEpisodes }
    }
    val backupLabel = remember(trackingSettings.lastBackupAtMs) {
        if (trackingSettings.lastBackupAtMs <= 0L) "No local backup exported yet"
        else "Last backup: " + SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            .format(Date(trackingSettings.lastBackupAtMs))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("sync_rpc_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Bookmarks,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Local Tracking & Presence", style = MaterialTheme.typography.headlineMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Zero API Keys",
                            tint = SimklEmerald,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Zero API Keys • On-Device Room Watchlist",
                            style = MaterialTheme.typography.labelSmall,
                            color = SimklEmerald
                        )
                    }
                }
            }
        }

        if (!statusBanner.isNullOrBlank()) {
            item {
                Card(
                    onClick = onDismissBanner,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$statusBanner (Tap to dismiss)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Discord Rich Presence Live Preview Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, DiscordBlurple.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.SportsEsports, contentDescription = null, tint = DiscordBlurple)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Discord Rich Presence", style = MaterialTheme.typography.titleMedium)
                        }
                        Switch(
                            checked = discordRpc.enabled,
                            onCheckedChange = { enabled -> onUpdateDiscordRpc { it.copy(enabled = enabled) } },
                            modifier = Modifier.testTag("discord_rpc_switch")
                        )
                    }

                    Surface(
                        color = Color(0xFF111214),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, Color(0xFF2B2D31)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(
                                    id = if (discordRpc.useCustomMaddookieArt) R.drawable.img_discord_rpc_art
                                    else R.drawable.ic_maddookie_logo
                                ),
                                contentDescription = stringResource(R.string.cd_discord_art),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(MaterialTheme.shapes.small)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (discordRpc.enabled) "MADDOOKIE • LIVE ACTIVITY" else "PRESENCE PAUSED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DiscordBlurple
                                )
                                Text(
                                    text = discordRpc.activeAnimeTitle.ifBlank { discordRpc.statusHeadline },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                                if (discordRpc.showEpisodeProgress && discordRpc.activeEpisode > 0) {
                                    val epText = buildString {
                                        append("Episode ${discordRpc.activeEpisode}")
                                        discordRpc.activeTotalEpisodes?.let { append("/$it") }
                                        if (discordRpc.showProviderBadge && discordRpc.activeProvider.isNotBlank()) {
                                            append(" • ${discordRpc.activeProvider} (${discordRpc.activeAudioMode})")
                                        }
                                    }
                                    Text(
                                        text = epText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFB5BAC1)
                                    )
                                } else {
                                    Text(
                                        text = "Potato-Turbo Low-RAM Anime Client",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFB5BAC1)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Episode Progress", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = discordRpc.showEpisodeProgress,
                            onCheckedChange = { v -> onUpdateDiscordRpc { it.copy(showEpisodeProgress = v) } }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Custom Maddookie Artwork", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = discordRpc.useCustomMaddookieArt,
                            onCheckedChange = { v -> onUpdateDiscordRpc { it.copy(useCustomMaddookieArt = v) } }
                        )
                    }
                }
            }
        }

        // Local Watchlist Stats & 85% Auto-Track
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("On-Device Watchlist Summary", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile("Watching", watchingCount.toString(), Modifier.weight(1f))
                        StatTile("Completed", completedCount.toString(), Modifier.weight(1f))
                        StatTile("Episodes", totalEpisodes.toString(), Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("Auto-Track at 85% Watched", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Automatically marks an episode watched in Room when playback reaches 85%.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = trackingSettings.autoTrackAt85Percent,
                            onCheckedChange = onToggleAutoTrack85,
                            modifier = Modifier.testTag("auto_track_85_switch")
                        )
                    }
                }
            }
        }

        // Local JSON Backup & Restore
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, TurboLime.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Local JSON Backup & Restore", style = MaterialTheme.typography.titleMedium)
                    Text(
                        backupLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onExportJsonClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_watchlist_json_button")
                        ) {
                            Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON")
                        }
                        OutlinedButton(
                            onClick = onImportJsonClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_watchlist_json_button")
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import JSON")
                        }
                    }
                }
            }
        }

        // Keyless Public Catalog Info
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Public, contentDescription = null, tint = SimklEmerald)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Keyless Public Catalog Mode", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Browse Trending, Seasonal, Search, and Airing Schedules with zero OAuth credentials, zero API keys, and zero external account logins.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
