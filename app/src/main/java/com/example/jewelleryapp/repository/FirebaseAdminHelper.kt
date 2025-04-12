package com.example.jewelleryapp.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Helper class for admin operations to update cache version
 * This would typically be used in your admin application, not the user app.
 * Including it here for reference.
 */
class FirebaseAdminHelper(private val firestore: FirebaseFirestore) {
    private val TAG = "FirebaseAdminHelper"

    /**
     * Updates the cache version after making changes to data in Firestore
     * Call this whenever you update products, categories, etc. from the admin app
     */
    suspend fun incrementCacheVersion() {
        try {
            // Get current version
            val docRef = firestore.collection(CacheManager.METADATA_COLLECTION)
                .document(CacheManager.CACHE_CONTROL_DOCUMENT)

            val document = docRef.get().await()
            val currentVersion = document.getString(CacheManager.VERSION_FIELD)?.toIntOrNull() ?: 0

            // Increment version
            val newVersion = (currentVersion + 1).toString()

            // Update in Firestore
            docRef.set(mapOf(CacheManager.VERSION_FIELD to newVersion)).await()

            Log.d(TAG, "Cache version incremented from $currentVersion to $newVersion")
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing cache version", e)
            throw e
        }
    }

    /**
     * Create the initial cache control document if it doesn't exist
     */
    suspend fun initializeCacheControlIfNeeded() {
        try {
            val docRef = firestore.collection(CacheManager.METADATA_COLLECTION)
                .document(CacheManager.CACHE_CONTROL_DOCUMENT)

            val document = docRef.get().await()

            if (!document.exists()) {
                // Create initial document with version 1
                docRef.set(mapOf(CacheManager.VERSION_FIELD to "1")).await()
                Log.d(TAG, "Cache control document initialized with version 1")
            } else {
                Log.d(TAG, "Cache control document already exists with version: ${document.getString(CacheManager.VERSION_FIELD)}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing cache control", e)
            throw e
        }
    }

    /**
     * Example usage for batch updating data and incrementing cache version
     */
    suspend fun updateProductsAndIncrementCache(updates: Map<String, Any>) {
        // Start a batch
        val batch = firestore.batch()

        try {
            // Apply updates
            updates.forEach { (productId, data) ->
                val productRef = firestore.collection("products").document(productId)
                batch.update(productRef, data as Map<String, Any>)
            }

            // Get the document reference for cache control
            val cacheControlRef = firestore.collection(CacheManager.METADATA_COLLECTION)
                .document(CacheManager.CACHE_CONTROL_DOCUMENT)

            // Get current version
            val document = cacheControlRef.get().await()
            val currentVersion = document.getString(CacheManager.VERSION_FIELD)?.toIntOrNull() ?: 0

            // Increment version
            val newVersion = (currentVersion + 1).toString()

            // Add cache version update to the batch
            batch.update(cacheControlRef, CacheManager.VERSION_FIELD, newVersion)

            // Commit the batch
            batch.commit().await()

            Log.d(TAG, "Updated ${updates.size} products and incremented cache version to $newVersion")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating products and cache version", e)
            throw e
        }
    }
}