package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.data.model.ProviderProbeState
import com.example.data.model.StreamAudioMode
import com.example.data.model.StreamProvider
import com.example.ui.theme.SimklEmerald
import com.example.ui.theme.TurboLime
import com.example.ui.viewmodel.PlayerUiState
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onBack: () -> Unit,
    onSwitchAudio: (StreamAudioMode) -> Unit,
    onSwitchProvider: (StreamProvider) -> Unit,
    onChangeEpisode: (Int) -> Unit,
    onRetry: () -> Unit,
    onProgressTick: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resolved = state.resolvedStream

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(resolved?.url, resolved?.headers) {
        if (resolved != null) {
            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .setDefaultRequestProperties(resolved.headers)
            val mediaSource = DefaultMediaSourceFactory(httpFactory)
                .createMediaSource(MediaItem.fromUri(resolved.url))
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } else {
            exoPlayer.stop()
        }
    }

    // Monitor playback progress for automatic 85% AniList/MAL/Simkl sync
    LaunchedEffect(resolved?.url, state.episode) {
        while (resolved != null) {
            if (exoPlayer.isPlaying && exoPlayer.duration > 0L) {
                onProgressTick(exoPlayer.currentPosition, exoPlayer.duration)
            }
            delay(1500L)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("player_screen"),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("player_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.anime?.displayTitle ?: "Maddookie Player",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Episode ${state.episode} • ${state.audioMode.label} • ${resolved?.provider?.displayName ?: "Resolving"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (resolved != null) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = true
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { view ->
                            if (view.player != exoPlayer) view.player = exoPlayer
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (state.isResolving) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Running Multi-Provider Fallback Engine…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.errorMessage ?: "Stream unavailable on selected provider",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onRetry,
                                modifier = Modifier.testTag("player_retry_button")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Chain")
                            }
                            val trailerUrl = state.anime?.trailerUrl
                            if (!trailerUrl.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl)))
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Trailer")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!state.syncStatusBanner.isNullOrBlank()) {
            item {
                Surface(
                    color = SimklEmerald.copy(alpha = 0.16f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = SimklEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.syncStatusBanner,
                            style = MaterialTheme.typography.labelLarge,
                            color = SimklEmerald
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onChangeEpisode((state.episode - 1).coerceAtLeast(1)) },
                        enabled = state.episode > 1,
                        modifier = Modifier.testTag("player_prev_ep")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous episode")
                        Text("Prev")
                    }
                    OutlinedButton(
                        onClick = {
                            val target = (exoPlayer.currentPosition + 85_000L)
                                .coerceAtMost(exoPlayer.duration.coerceAtLeast(0L))
                            exoPlayer.seekTo(target)
                        },
                        enabled = resolved != null,
                        modifier = Modifier.testTag("player_skip_op")
                    ) {
                        Icon(Icons.Filled.FastForward, contentDescription = "Skip 85 seconds")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+85s OP")
                    }
                    Button(
                        onClick = { onChangeEpisode(state.episode + 1) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("player_next_ep")
                    ) {
                        Text("Next Ep")
                        Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next episode")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Audio Stream",
                        style = MaterialTheme.typography.titleMedium
                    )
                    SingleChoiceSegmentedButtonRow {
                        StreamAudioMode.entries.forEachIndexed { idx, mode ->
                            SegmentedButton(
                                selected = state.audioMode == mode,
                                onClick = { onSwitchAudio(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = idx,
                                    count = StreamAudioMode.entries.size
                                ),
                                modifier = Modifier.testTag("player_audio_${mode.label}")
                            ) {
                                Text(mode.label)
                            }
                        }
                    }
                }

                Text(
                    text = "Multi-Provider Fallback Engine (5 Sources)",
                    style = MaterialTheme.typography.titleMedium
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(StreamProvider.entries) { provider ->
                        val active = resolved?.provider == provider || state.preferredProvider == provider
                        FilterChip(
                            selected = active,
                            onClick = { onSwitchProvider(provider) },
                            label = { Text(provider.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("provider_chip_${provider.name}")
                        )
                    }
                }

                if (state.attempts.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Live Provider Pipeline Log",
                                style = MaterialTheme.typography.titleSmall
                            )
                            state.attempts.forEach { attempt ->
                                val statusColor = when (attempt.state) {
                                    ProviderProbeState.SUCCESS -> TurboLime
                                    ProviderProbeState.CHECKING -> MaterialTheme.colorScheme.secondary
                                    ProviderProbeState.FAILED -> MaterialTheme.colorScheme.error
                                    ProviderProbeState.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = attempt.provider.displayName,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Text(
                                            text = attempt.detail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        color = statusColor.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (attempt.latencyMs > 0L) {
                                                "${attempt.state.name} (${attempt.latencyMs}ms)"
                                            } else attempt.state.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
