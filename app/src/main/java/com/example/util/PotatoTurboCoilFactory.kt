package com.example.util

import android.content.Context
import android.graphics.Bitmap
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.model.PotatoTurboConfig
import okhttp3.OkHttpClient

object PotatoTurboCoilFactory {
    fun build(
        context: Context,
        okHttpClient: OkHttpClient,
        config: PotatoTurboConfig
    ): ImageLoader {
        val capPercent = (config.heapCapPercent.coerceIn(6, 25)) / 100.0
        return ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .bitmapConfig(if (config.rgb565Enabled) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888)
            .allowRgb565(config.rgb565Enabled)
            .crossfade(!config.zeroBlurFastPath)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(capPercent)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("maddookie_image_cache"))
                    .maxSizeBytes(64L * 1024L * 1024L)
                    .build()
            }
            .build()
    }
}
