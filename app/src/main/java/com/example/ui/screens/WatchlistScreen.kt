package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.WatchlistEntity
import com.example.data.model.WatchStatus
import com.example.ui.theme.AniListCyan
import com.example.ui.theme.MalBlue
import com.example.ui.theme.SimklEmerald

@Composable
fun WatchlistScreen(
    entries: List<WatchlistEntity>,
    onOpenAnime: (Int) -> Unit,
    onPlayNext: (Int, Int) -> Unit,
    onIncrementEpisode: (WatchlistEntity) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<WatchStatus?>(null) }

    val filtered = remember(entries, selectedFilter) {
        if (selectedFilter == null) entries
        else entries.filter { it.statusEnum() == selectedFilter }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("watchlist_screen")
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Bookmarks,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "My Watchlist",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("All (${entries.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("watchlist_filter_all")
                    )
                }
                items(WatchStatus.entries) { status ->
                    val count = entries.count { it.statusEnum() == status }
                    FilterChip(
                        selected = selectedFilter == status,
                        onClick = { selectedFilter = status },
                        label = { Text("${status.label} ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("watchlist_filter_${status.name}")
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Bookmarks,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your Watchlist is Empty",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Open any anime from Discover or Schedule to track episodes and auto-sync at 85% watched.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.animeId }) { entry ->
                    Card(
                        onClick = { onOpenAnime(entry.animeId) },
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("watchlist_card_${entry.animeId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = entry.coverUrl,
                                contentDescription = entry.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 64.dp, height = 90.dp)
                                    .clip(MaterialTheme.shapes.small)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${entry.statusEnum().label} • ${entry.watchedEpisodes}/${entry.totalEpisodes ?: "?"} ep",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val ratio = entry.totalEpisodes?.let { total ->
                                    if (total > 0) (entry.watchedEpisodes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                                    else 0f
                                } ?: 0f
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (entry.syncedToAniList) {
                                        SyncTagChip("AniList", AniListCyan)
                                    }
                                    if (entry.syncedToMal) {
                                        SyncTagChip("MAL", MalBlue)
                                    }
                                    if (entry.syncedToSimkl) {
                                        SyncTagChip("Simkl", SimklEmerald)
                                    }
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilledTonalIconButton(
                                    onClick = {
                                        val nextEp = (entry.watchedEpisodes + 1)
                                            .coerceAtMost(entry.totalEpisodes ?: Int.MAX_VALUE)
                                        onPlayNext(entry.animeId, nextEp)
                                    },
                                    modifier = Modifier.testTag("watchlist_play_${entry.animeId}")
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = "Watch next episode")
                                }
                                Row {
                                    IconButton(
                                        onClick = { onIncrementEpisode(entry) },
                                        modifier = Modifier.testTag("watchlist_inc_${entry.animeId}")
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Increment watched episode")
                                    }
                                    IconButton(
                                        onClick = { onRemove(entry.animeId) },
                                        modifier = Modifier.testTag("watchlist_remove_${entry.animeId}")
                                    ) {
                                        Icon(
                                            Icons.Filled.DeleteOutline,
                                            contentDescription = "Remove from watchlist",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncTagChip(label: String, tint: androidx.compose.ui.graphics.Color) {
    Surface(
        color = tint.copy(alpha = 0.16f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
