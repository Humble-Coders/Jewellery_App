package com.example.jewelleryapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import com.example.jewelleryapp.repository.CacheManager
import com.example.jewelleryapp.repository.FirebaseAdminHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class JewelryApplication : Application(), ImageLoaderFactory {
    // Create CacheManager as a singleton
    val cacheManager by lazy {
        CacheManager(applicationContext, FirebaseFirestore.getInstance())
    }

    // Create FirebaseAdminHelper for admin operations (normally not used in user app)
    private val adminHelper by lazy {
        FirebaseAdminHelper(FirebaseFirestore.getInstance())
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize cache control document if needed
        CoroutineScope(Dispatchers.IO).launch {
            try {
                adminHelper.initializeCacheControlIfNeeded()
            } catch (e: Exception) {
                // Log but don't crash if this fails
                e.printStackTrace()
            }
        }
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