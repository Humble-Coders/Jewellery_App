package com.example.jewelleryapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger

class JewelryApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        // Any other app initialization code
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.25) // Use 25% of available disk space
                    .build()
            }
            .memoryCache {
                MemoryCache.Builder(this) // Pass 'this' as context to MemoryCache.Builder
                    .maxSizePercent(0.25) // Use 25% of app memory
                    .build()
            }
            .crossfade(true) // Enable crossfade animation between images
            .respectCacheHeaders(false) // Ignore cache headers from network
            .build()
    }
}