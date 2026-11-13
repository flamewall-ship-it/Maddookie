package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.WatchlistEntity
import com.example.data.model.AnimeMedia
import com.example.data.model.StreamAudioMode
import com.example.data.model.WatchStatus
import com.example.ui.theme.AmberSecondary
import com.example.ui.viewmodel.UiLoadState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimeDetailScreen(
    detailState: UiLoadState<AnimeMedia>,
    watchlistEntry: WatchlistEntity?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onUpdateStatus: (AnimeMedia, WatchStatus) -> Unit,
    onPlayEpisode: (Int, Int, StreamAudioMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedAudioMode by remember { mutableStateOf(StreamAudioMode.SUB) }
    var expandDescription by remember { mutableStateOf(false) }

    when (detailState) {
        is UiLoadState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiLoadState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(detailState.message, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onBack) { Text("Back") }
                        Button(onClick = onRetry) { Text("Retry") }
                    }
                }
            }
        }
        is UiLoadState.Success -> {
            val media = detailState.data
            val totalEps = media.episodes ?: media.nextAiringEpisode?.episode?.minus(1)?.coerceAtLeast(1) ?: 12
            val watchedCount = watchlistEntry?.watchedEpisodes ?: 0
            val nextEp = (watchedCount + 1).coerceAtMost(totalEps.coerceAtLeast(1))

            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .testTag("anime_detail_screen"),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    ) {
                        AsyncImage(
                            model = media.bannerUrl ?: media.coverUrl,
                            contentDescription = media.displayTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.35f),
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                                            MaterialTheme.colorScheme.background
                                        )
                                    )
                                )
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(14.dp)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("detail_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            AsyncImage(
                                model = media.coverUrl,
                                contentDescription = media.displayTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 96.dp, height = 136.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = media.displayTitle,
                                    style = MaterialTheme.typography.headlineSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (media.studios.isNotEmpty()) {
                                    Text(
                                        text = media.studios.joinToString(", "),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    media.averageScore?.let { score ->
                                        Surface(
                                            color = AmberSecondary.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Star,
                                                    contentDescription = null,
                                                    tint = AmberSecondary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "${score}%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AmberSecondary
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = listOfNotNull(
                                            media.format,
                                            media.seasonYear?.toString(),
                                            media.episodes?.let { "$it ep" }
                                        ).joinToString(" • "),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onPlayEpisode(media.id, nextEp, selectedAudioMode) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("detail_play_button")
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (watchedCount > 0) "Resume Ep $nextEp" else "Watch Ep 1"
                                )
                            }

                            SingleChoiceSegmentedButtonRow {
                                StreamAudioMode.entries.forEachIndexed { idx, mode ->
                                    SegmentedButton(
                                        selected = selectedAudioMode == mode,
                                        onClick = { selectedAudioMode = mode },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = idx,
                                            count = StreamAudioMode.entries.size
                                        ),
                                        modifier = Modifier.testTag("detail_audio_${mode.label}")
                                    ) {
                                        Text(mode.label)
                                    }
                                }
                            }
                        }

                        if (!media.trailerUrl.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(media.trailerUrl)))
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detail_trailer_button")
                            ) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Official Trailer")
                            }
                        }

                        Text(
                            text = "Track Status (Auto-syncs to AniList / MAL / Simkl)",
                            style = MaterialTheme.typography.titleSmall
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(WatchStatus.entries) { status ->
                                val isSelected = watchlistEntry?.statusEnum() == status
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onUpdateStatus(media, status) },
                                    label = { Text(status.label) },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("detail_status_${status.name}")
                                )
                            }
                        }

                        if (media.genres.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                media.genres.forEach { genre ->
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(genre, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }

                        if (media.description.isNotBlank()) {
                            Card(
                                onClick = { expandDescription = !expandDescription },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = media.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = if (expandDescription) 40 else 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (expandDescription) "Show less" else "Read full synopsis",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Episodes ($totalEps)",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }

                val episodeNumbers = (1..totalEps.coerceAtLeast(1)).toList()
                items(episodeNumbers) { ep ->
                    val watched = ep <= watchedCount
                    Card(
                        onClick = { onPlayEpisode(media.id, ep, selectedAudioMode) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (watched) {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (watched) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("episode_row_$ep")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (watched) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "EP $ep",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (watched) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Episode $ep",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = if (watched) "Watched (85%+ synced)" else "Multi-Provider • ${selectedAudioMode.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.cd_play_episode),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
