package com.example.jewelleryapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import com.example.jewelleryapp.repository.CacheManager
import com.example.jewelleryapp.repository.CachedJewelryRepository
import com.example.jewelleryapp.repository.FirebaseAdminHelper
import com.example.jewelleryapp.repository.FirebaseStorageHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    // Add a repository for data refreshing
    // Create repository
    val jewelryRepository by lazy {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        CachedJewelryRepository(
            userId,
            FirebaseFirestore.getInstance(),
            FirebaseStorageHelper(),
            cacheManager,
            applicationContext
        )
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize with a delay to avoid blocking app startup
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // First check if cache control exists
                adminHelper.initializeCacheControlIfNeeded()

                // Then check if cache needs refresh (without saving version prematurely)
                checkCacheVersion()

            } catch (e: Exception) {
                // Log but don't crash if this fails
                e.printStackTrace()
            }
        }
    }
    private suspend fun checkCacheVersion() {
        if (cacheManager.shouldRefreshCache()) {
            // If refresh is needed, refresh all data at once
            (jewelryRepository as? CachedJewelryRepository)?.refreshData()
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
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use 25% of app memory
                    .build()
            }
            .crossfade(true) // Enable crossfade animation between images
            .respectCacheHeaders(false) // Ignore cache headers from network
            .build()
    }
}