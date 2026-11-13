package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DiscordRpcConfig
import com.example.data.model.SyncAccountState
import com.example.ui.theme.AniListCyan
import com.example.ui.theme.DiscordBlurple
import com.example.ui.theme.MalBlue
import com.example.ui.theme.SimklEmerald

@Composable
fun SyncRpcScreen(
    syncState: SyncAccountState,
    discordRpc: DiscordRpcConfig,
    authBanner: String?,
    onSaveClientIds: (String, String, String) -> Unit,
    onStartAniListOAuth: () -> Unit,
    onConnectAniListToken: (String) -> Unit,
    onStartMalOAuth: () -> Unit,
    onRequestSimklPin: () -> Unit,
    onVerifySimklPin: () -> Unit,
    onDisconnect: (String) -> Unit,
    onToggleAutoSync85: (Boolean) -> Unit,
    onUpdateDiscordRpc: ((DiscordRpcConfig) -> DiscordRpcConfig) -> Unit,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var alClientId by remember(syncState.anilistClientId) { mutableStateOf(syncState.anilistClientId) }
    var malClientId by remember(syncState.malClientId) { mutableStateOf(syncState.malClientId) }
    var simklClientId by remember(syncState.simklClientId) { mutableStateOf(syncState.simklClientId) }
    var manualAniListToken by remember { mutableStateOf("") }

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
                    imageVector = Icons.Filled.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Two-Way Sync & Rich Presence", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "AniList • MyAnimeList • Simkl PIN • Discord RPC",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (!authBanner.isNullOrBlank()) {
            item {
                Card(
                    onClick = onDismissBanner,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$authBanner (Tap to dismiss)",
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

        // Auto-sync at 85% card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Sync Progress at 85% Watched", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Automatically updates AniList, MAL, and Simkl when playback reaches 85%.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = syncState.autoSyncAt85Percent,
                        onCheckedChange = onToggleAutoSync85,
                        modifier = Modifier.testTag("auto_sync_85_switch")
                    )
                }
            }
        }

        // AniList OAuth2 PKCE + Token
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, AniListCyan.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AniList OAuth2 PKCE", style = MaterialTheme.typography.titleMedium, color = AniListCyan)
                        if (syncState.anilistConnected) {
                            ConnectedPill(syncState.anilistUsername, AniListCyan)
                        }
                    }
                    Text(
                        "Redirect URI: maddookie://auth",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = alClientId,
                        onValueChange = { alClientId = it },
                        label = { Text("AniList Client ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onSaveClientIds(alClientId, malClientId, simklClientId) },
                            modifier = Modifier.testTag("save_oauth_ids_button")
                        ) {
                            Text("Save ID")
                        }
                        Button(
                            onClick = {
                                onSaveClientIds(alClientId, malClientId, simklClientId)
                                onStartAniListOAuth()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AniListCyan),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("anilist_oauth_button")
                        ) {
                            Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Authorize AniList")
                        }
                    }
                    OutlinedTextField(
                        value = manualAniListToken,
                        onValueChange = { manualAniListToken = it },
                        label = { Text("Or Paste AniList Access Token") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onConnectAniListToken(manualAniListToken) },
                            enabled = manualAniListToken.isNotBlank(),
                            modifier = Modifier.testTag("anilist_token_connect_button")
                        ) {
                            Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify Token")
                        }
                        if (syncState.anilistConnected) {
                            OutlinedButton(onClick = { onDisconnect("anilist") }) { Text("Disconnect") }
                        }
                    }
                }
            }
        }

        // MyAnimeList OAuth2 PKCE
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MalBlue.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("MyAnimeList OAuth2 PKCE", style = MaterialTheme.typography.titleMedium, color = MalBlue)
                        if (syncState.malConnected) {
                            ConnectedPill(syncState.malUsername, MalBlue)
                        }
                    }
                    Text(
                        "Redirect URI: maddookie://mal-auth",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = malClientId,
                        onValueChange = { malClientId = it },
                        label = { Text("MAL Client ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onSaveClientIds(alClientId, malClientId, simklClientId)
                                onStartMalOAuth()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MalBlue),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mal_oauth_button")
                        ) {
                            Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Authorize MAL")
                        }
                        if (syncState.malConnected) {
                            OutlinedButton(onClick = { onDisconnect("mal") }) { Text("Disconnect") }
                        }
                    }
                }
            }
        }

        // Simkl Redirect-Free Device PIN
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, SimklEmerald.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Simkl Device PIN", style = MaterialTheme.typography.titleMedium, color = SimklEmerald)
                        if (syncState.simklConnected) {
                            ConnectedPill("Synced", SimklEmerald)
                        }
                    }
                    Text(
                        "Redirect-free TV & mobile auth via simkl.com/pin",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = simklClientId,
                        onValueChange = { simklClientId = it },
                        label = { Text("Simkl Client ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (syncState.simklUserCode.isNotBlank()) {
                        Surface(
                            color = SimklEmerald.copy(alpha = 0.14f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("PIN CODE", style = MaterialTheme.typography.labelSmall, color = SimklEmerald)
                                    Text(syncState.simklUserCode, style = MaterialTheme.typography.headlineMedium)
                                }
                                OutlinedButton(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse("https://simkl.com/pin"))
                                            )
                                        }
                                    }
                                ) {
                                    Text("Open simkl.com/pin")
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onSaveClientIds(alClientId, malClientId, simklClientId)
                                onRequestSimklPin()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SimklEmerald,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("simkl_request_pin_button")
                        ) {
                            Icon(Icons.Filled.Pin, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate PIN")
                        }
                        if (syncState.simklUserCode.isNotBlank()) {
                            OutlinedButton(
                                onClick = onVerifySimklPin,
                                modifier = Modifier.testTag("simkl_verify_pin_button")
                            ) {
                                Text("Verify PIN")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectedPill(label: String, tint: Color) {
    Surface(
        color = tint.copy(alpha = 0.16f),
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
        }
    }
}
