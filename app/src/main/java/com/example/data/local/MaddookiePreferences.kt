package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.DiscordRpcConfig
import com.example.data.model.LocalTrackingSettings
import com.example.data.model.PotatoTurboConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.maddookieDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "maddookie_prefs"
)

class MaddookiePreferences(private val context: Context) {

    private object Keys {
        val TURBO_RGB565 = booleanPreferencesKey("turbo_rgb565")
        val TURBO_HEAP_CAP = intPreferencesKey("turbo_heap_cap")
        val TURBO_DEFERRED_WEBVIEW = booleanPreferencesKey("turbo_deferred_webview")
        val TURBO_ZERO_BLUR = booleanPreferencesKey("turbo_zero_blur")
        val DARK_THEME = booleanPreferencesKey("dark_theme")

        val AUTO_TRACK_85 = booleanPreferencesKey("auto_track_85")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")

        val RPC_ENABLED = booleanPreferencesKey("rpc_enabled")
        val RPC_SHOW_EP = booleanPreferencesKey("rpc_show_ep")
        val RPC_SHOW_PROVIDER = booleanPreferencesKey("rpc_show_provider")
        val RPC_CUSTOM_ART = booleanPreferencesKey("rpc_custom_art")
        val RPC_HEADLINE = stringPreferencesKey("rpc_headline")
        val RPC_ACTIVE_TITLE = stringPreferencesKey("rpc_active_title")
        val RPC_ACTIVE_EP = intPreferencesKey("rpc_active_ep")
        val RPC_ACTIVE_TOTAL = intPreferencesKey("rpc_active_total")
        val RPC_ACTIVE_PROVIDER = stringPreferencesKey("rpc_active_provider")
        val RPC_ACTIVE_AUDIO = stringPreferencesKey("rpc_active_audio")
        val RPC_LAST_UPDATED = longPreferencesKey("rpc_last_updated")
    }

    val turboConfig: Flow<PotatoTurboConfig> = context.maddookieDataStore.data.map { prefs ->
        PotatoTurboConfig(
            rgb565Enabled = prefs[Keys.TURBO_RGB565] ?: true,
            heapCapPercent = prefs[Keys.TURBO_HEAP_CAP] ?: 12,
            deferredWebViewEnabled = prefs[Keys.TURBO_DEFERRED_WEBVIEW] ?: true,
            zeroBlurFastPath = prefs[Keys.TURBO_ZERO_BLUR] ?: true,
            darkTheme = prefs[Keys.DARK_THEME] ?: true
        )
    }

    val localTrackingSettings: Flow<LocalTrackingSettings> = context.maddookieDataStore.data.map { prefs ->
        LocalTrackingSettings(
            autoTrackAt85Percent = prefs[Keys.AUTO_TRACK_85] ?: true,
            lastBackupAtMs = prefs[Keys.LAST_BACKUP_AT] ?: 0L
        )
    }

    val discordRpcConfig: Flow<DiscordRpcConfig> = context.maddookieDataStore.data.map { prefs ->
        val total = prefs[Keys.RPC_ACTIVE_TOTAL] ?: 0
        DiscordRpcConfig(
            enabled = prefs[Keys.RPC_ENABLED] ?: true,
            showEpisodeProgress = prefs[Keys.RPC_SHOW_EP] ?: true,
            showProviderBadge = prefs[Keys.RPC_SHOW_PROVIDER] ?: true,
            useCustomMaddookieArt = prefs[Keys.RPC_CUSTOM_ART] ?: true,
            statusHeadline = prefs[Keys.RPC_HEADLINE] ?: "Streaming on Maddookie",
            activeAnimeTitle = prefs[Keys.RPC_ACTIVE_TITLE] ?: "",
            activeEpisode = prefs[Keys.RPC_ACTIVE_EP] ?: 0,
            activeTotalEpisodes = if (total > 0) total else null,
            activeProvider = prefs[Keys.RPC_ACTIVE_PROVIDER] ?: "",
            activeAudioMode = prefs[Keys.RPC_ACTIVE_AUDIO] ?: "SUB",
            lastSyncedTimestamp = prefs[Keys.RPC_LAST_UPDATED] ?: 0L
        )
    }

    suspend fun updateTurboConfig(transform: (PotatoTurboConfig) -> PotatoTurboConfig) {
        context.maddookieDataStore.edit { prefs ->
            val current = PotatoTurboConfig(
                rgb565Enabled = prefs[Keys.TURBO_RGB565] ?: true,
                heapCapPercent = prefs[Keys.TURBO_HEAP_CAP] ?: 12,
                deferredWebViewEnabled = prefs[Keys.TURBO_DEFERRED_WEBVIEW] ?: true,
                zeroBlurFastPath = prefs[Keys.TURBO_ZERO_BLUR] ?: true,
                darkTheme = prefs[Keys.DARK_THEME] ?: true
            )
            val next = transform(current)
            prefs[Keys.TURBO_RGB565] = next.rgb565Enabled
            prefs[Keys.TURBO_HEAP_CAP] = next.heapCapPercent.coerceIn(6, 25)
            prefs[Keys.TURBO_DEFERRED_WEBVIEW] = next.deferredWebViewEnabled
            prefs[Keys.TURBO_ZERO_BLUR] = next.zeroBlurFastPath
            prefs[Keys.DARK_THEME] = next.darkTheme
        }
    }

    suspend fun setAutoTrack85(enabled: Boolean) {
        context.maddookieDataStore.edit { prefs ->
            prefs[Keys.AUTO_TRACK_85] = enabled
        }
    }

    suspend fun setLastBackupAt(timestampMs: Long) {
        context.maddookieDataStore.edit { prefs ->
            prefs[Keys.LAST_BACKUP_AT] = timestampMs
        }
    }

    suspend fun updateDiscordRpc(transform: (DiscordRpcConfig) -> DiscordRpcConfig) {
        context.maddookieDataStore.edit { prefs ->
            val total = prefs[Keys.RPC_ACTIVE_TOTAL] ?: 0
            val current = DiscordRpcConfig(
                enabled = prefs[Keys.RPC_ENABLED] ?: true,
                showEpisodeProgress = prefs[Keys.RPC_SHOW_EP] ?: true,
                showProviderBadge = prefs[Keys.RPC_SHOW_PROVIDER] ?: true,
                useCustomMaddookieArt = prefs[Keys.RPC_CUSTOM_ART] ?: true,
                statusHeadline = prefs[Keys.RPC_HEADLINE] ?: "Streaming on Maddookie",
                activeAnimeTitle = prefs[Keys.RPC_ACTIVE_TITLE] ?: "",
                activeEpisode = prefs[Keys.RPC_ACTIVE_EP] ?: 0,
                activeTotalEpisodes = if (total > 0) total else null,
                activeProvider = prefs[Keys.RPC_ACTIVE_PROVIDER] ?: "",
                activeAudioMode = prefs[Keys.RPC_ACTIVE_AUDIO] ?: "SUB",
                lastSyncedTimestamp = prefs[Keys.RPC_LAST_UPDATED] ?: 0L
            )
            val next = transform(current)
            prefs[Keys.RPC_ENABLED] = next.enabled
            prefs[Keys.RPC_SHOW_EP] = next.showEpisodeProgress
            prefs[Keys.RPC_SHOW_PROVIDER] = next.showProviderBadge
            prefs[Keys.RPC_CUSTOM_ART] = next.useCustomMaddookieArt
            prefs[Keys.RPC_HEADLINE] = next.statusHeadline
            prefs[Keys.RPC_ACTIVE_TITLE] = next.activeAnimeTitle
            prefs[Keys.RPC_ACTIVE_EP] = next.activeEpisode
            prefs[Keys.RPC_ACTIVE_TOTAL] = next.activeTotalEpisodes ?: 0
            prefs[Keys.RPC_ACTIVE_PROVIDER] = next.activeProvider
            prefs[Keys.RPC_ACTIVE_AUDIO] = next.activeAudioMode
            prefs[Keys.RPC_LAST_UPDATED] = next.lastSyncedTimestamp
        }
    }
}
