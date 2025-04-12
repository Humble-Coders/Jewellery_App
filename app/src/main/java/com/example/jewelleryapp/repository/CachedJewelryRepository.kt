package com.example.jewelleryapp.repository

import android.content.Context
import android.util.Log
import com.example.jewelleryapp.model.CarouselItem
import com.example.jewelleryapp.model.Category
import com.example.jewelleryapp.model.Collection
import com.example.jewelleryapp.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Extension of JewelryRepository that implements caching with version control
 */
class CachedJewelryRepository(
    private val userId: String,
    private val firestore: FirebaseFirestore,
    private val storageHelper: FirebaseStorageHelper,
    private val cacheManager: CacheManager,
    private val context: Context
) : JewelryRepository(userId, firestore, storageHelper) {
    private val TAG = "CachedJewelryRepository"

    // Cache containers
    private val categoriesCache = MutableStateFlow<List<Category>>(emptyList())
    private val featuredProductsCache = MutableStateFlow<List<Product>>(emptyList())
    private val collectionsCache = MutableStateFlow<List<Collection>>(emptyList())
    private val carouselItemsCache = MutableStateFlow<List<CarouselItem>>(emptyList())

    // Flag to prevent multiple simultaneous refreshes
    private var isRefreshing = false

    init {
        // Setup listener for cache version changes
        setupCacheVersionListener()
    }

    // In CachedJewelryRepository.kt - setupCacheVersionListener method
    private fun setupCacheVersionListener() {
        cacheManager.listenForCacheVersionChanges { serverVersion ->
            CoroutineScope(Dispatchers.IO).launch {
                val localVersion = cacheManager.getLocalCacheVersion().first()
                if (serverVersion != localVersion) {
                    Log.d(TAG, "Cache version changed from $localVersion to $serverVersion, refreshing data")
                    refreshData()
                    // Update local version after refresh
                    cacheManager.saveLocalCacheVersion(serverVersion)
                }
            }
        }
    }

    // Refreshes all cached data from Firestore
    suspend fun refreshData() {
        if (isRefreshing) {
            Log.d(TAG, "Data refresh already in progress, skipping")
            return
        }

        try {
            isRefreshing = true
            Log.d(TAG, "Starting data refresh from Firestore")

            // Refresh categories
            withContext(Dispatchers.IO) {
                try {
                    super.getCategories().collect { categories ->
                        categoriesCache.value = categories
                        Log.d(TAG, "Refreshed ${categories.size} categories")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing categories", e)
                }
            }

            // Refresh featured products
            withContext(Dispatchers.IO) {
                try {
                    // Make sure wishlist cache is updated first
                    refreshWishlistCache()

                    super.getFeaturedProducts().collect { products ->
                        featuredProductsCache.value = products
                        Log.d(TAG, "Refreshed ${products.size} featured products")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing featured products", e)
                }
            }

            // Refresh collections
            withContext(Dispatchers.IO) {
                try {
                    super.getThemedCollections().collect { collections ->
                        collectionsCache.value = collections
                        Log.d(TAG, "Refreshed ${collections.size} collections")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing collections", e)
                }
            }

            // Refresh carousel items
            withContext(Dispatchers.IO) {
                try {
                    super.getCarouselItems().collect { items ->
                        carouselItemsCache.value = items
                        Log.d(TAG, "Refreshed ${items.size} carousel items")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing carousel items", e)
                }
            }

            // Update metadata document in Firestore to get latest cache version
            // From the refreshData() method in CachedJewelryRepository.kt
            val docRef = firestore.collection(CacheManager.METADATA_COLLECTION)
                .document(CacheManager.CACHE_CONTROL_DOCUMENT)
            val document = docRef.get().await()
            val serverVersion = document.getString(CacheManager.VERSION_FIELD) ?: "0"

// Save the latest version locally
            cacheManager.saveLocalCacheVersion(serverVersion)
            Log.d(TAG, "Data refresh complete, updated to version $serverVersion")

        } catch (e: Exception) {
            Log.e(TAG, "Error during data refresh", e)
        } finally {
            isRefreshing = false
        }
    }

    // Check if cache should be refreshed and refresh if needed
    private suspend fun checkAndRefreshCache() {
        try {
            if (cacheManager.shouldRefreshCache()) {
                Log.d(TAG, "Cache is outdated, refreshing data")
                refreshData()
            } else {
                Log.d(TAG, "Cache is current, using cached data")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking cache status", e)
        }
    }

    // Override methods to use cached data
    override suspend fun getCategories(): Flow<List<Category>> = flow {
        // First emit cached data if available
        val cachedData = categoriesCache.value
        if (cachedData.isNotEmpty()) {
            Log.d(TAG, "Emitting ${cachedData.size} categories from cache")
            emit(cachedData)
        } else {
            // If cache is empty, fetch from parent and cache the result
            Log.d(TAG, "Cache empty, fetching categories from Firestore")
            super.getCategories().collect { categories ->
                categoriesCache.value = categories
                emit(categories)
            }
        }

        // Check if cache needs refreshing
        checkAndRefreshCache()
    }

    override suspend fun getFeaturedProducts(): Flow<List<Product>> = flow {
        // First emit cached data if available
        val cachedData = featuredProductsCache.value
        if (cachedData.isNotEmpty()) {
            Log.d(TAG, "Emitting ${cachedData.size} featured products from cache")
            emit(cachedData)
        } else {
            // If cache is empty, fetch from parent and cache the result
            Log.d(TAG, "Cache empty, fetching featured products from Firestore")
            super.getFeaturedProducts().collect { products ->
                featuredProductsCache.value = products
                emit(products)
            }
        }

        // Check if cache needs refreshing
        checkAndRefreshCache()
    }

    override suspend fun getThemedCollections(): Flow<List<Collection>> = flow {
        // First emit cached data if available
        val cachedData = collectionsCache.value
        if (cachedData.isNotEmpty()) {
            Log.d(TAG, "Emitting ${cachedData.size} collections from cache")
            emit(cachedData)
        } else {
            // If cache is empty, fetch from parent and cache the result
            Log.d(TAG, "Cache empty, fetching collections from Firestore")
            super.getThemedCollections().collect { collections ->
                collectionsCache.value = collections
                emit(collections)
            }
        }

        // Check if cache needs refreshing
        checkAndRefreshCache()
    }

    override suspend fun getCarouselItems(): Flow<List<CarouselItem>> = flow {
        // First emit cached data if available
        val cachedData = carouselItemsCache.value
        if (cachedData.isNotEmpty()) {
            Log.d(TAG, "Emitting ${cachedData.size} carousel items from cache")
            emit(cachedData)
        } else {
            // If cache is empty, fetch from parent and cache the result
            Log.d(TAG, "Cache empty, fetching carousel items from Firestore")
            super.getCarouselItems().collect { items ->
                carouselItemsCache.value = items
                emit(items)
            }
        }

        // Check if cache needs refreshing
        checkAndRefreshCache()
    }

    // For wishlist operations, we need to update the cache immediately
    override suspend fun addToWishlist(productId: String) {
        super.addToWishlist(productId)

        // Update featured products cache
        featuredProductsCache.value = featuredProductsCache.value.map {
            if (it.id == productId) it.copy(isFavorite = true) else it
        }
    }

    override suspend fun removeFromWishlist(productId: String) {
        super.removeFromWishlist(productId)

        // Update featured products cache
        featuredProductsCache.value = featuredProductsCache.value.map {
            if (it.id == productId) it.copy(isFavorite = false) else it
        }
    }
}