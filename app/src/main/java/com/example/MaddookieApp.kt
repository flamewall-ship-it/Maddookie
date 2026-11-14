package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.data.local.MaddookieDatabase
import com.example.data.local.MaddookiePreferences
import com.example.data.local.WatchlistRepository
import com.example.data.model.PotatoTurboConfig
import com.example.data.remote.AniListClient
import com.example.data.remote.MultiProviderStreamingEngine
import com.example.util.PotatoTurboCoilFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MaddookieApp : Application(), ImageLoaderFactory {

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    val preferences: MaddookiePreferences by lazy {
        MaddookiePreferences(this)
    }

    val database: MaddookieDatabase by lazy {
        MaddookieDatabase.getInstance(this)
    }

    val watchlistRepository: WatchlistRepository by lazy {
        WatchlistRepository(database.watchlistDao())
    }

    val aniListClient: AniListClient by lazy {
        AniListClient(okHttpClient)
    }

    val streamingEngine: MultiProviderStreamingEngine by lazy {
        MultiProviderStreamingEngine(okHttpClient)
    }

    override fun newImageLoader(): ImageLoader {
        val initialTurbo = runCatching {
            runBlocking { preferences.turboConfig.first() }
        }.getOrDefault(PotatoTurboConfig())
        return PotatoTurboCoilFactory.build(this, okHttpClient, initialTurbo)
    }
}
