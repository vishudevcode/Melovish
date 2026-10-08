package com.melovish.player

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

class MelovishApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .allowHardware(true)
            .allowRgb565(true)
            // Allocates up to 30% of available JVM heap for ultra-fast, smooth in-memory cover caching
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30)
                    .strongReferencesEnabled(true)
                    .build()
            }
            // Dedicated high-speed disk cache directory to keep device media covers persistent and instant
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("melovish_art_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .respectCacheHeaders(false) // Ensures local device audio covers load immediately without HTTP/header checks
            .build()
    }
}
