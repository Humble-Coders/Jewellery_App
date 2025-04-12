package com.example.jewelleryapp.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

// Extension property for Context to create DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jewelry_cache")

/**
 * Manages cache versioning and data refreshing
 */
class CacheManager(
    private val context: Context,
    private val firestore: FirebaseFirestore
) {
    private val TAG = "CacheManager"

    companion object {
        private val CACHE_VERSION_KEY = stringPreferencesKey("cache_version")
        private val LAST_UPDATE_TIMESTAMP_KEY = longPreferencesKey("last_update_timestamp")

        // Firestore paths
        const val METADATA_COLLECTION = "metadata"
        const val CACHE_CONTROL_DOCUMENT = "cache_control"
        const val VERSION_FIELD = "version"
    }

    // Get cache version from DataStore
    fun getLocalCacheVersion(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[CACHE_VERSION_KEY] ?: "0"
        }
    }

    // Get last update timestamp from DataStore
    fun getLastUpdateTimestamp(): Flow<Long> {
        return context.dataStore.data.map { preferences ->
            preferences[LAST_UPDATE_TIMESTAMP_KEY] ?: 0L
        }
    }

    // Save cache version to DataStore
    suspend fun saveLocalCacheVersion(version: String) {
        context.dataStore.edit { preferences ->
            preferences[CACHE_VERSION_KEY] = version
            preferences[LAST_UPDATE_TIMESTAMP_KEY] = System.currentTimeMillis()
        }
        Log.d(TAG, "Saved local cache version: $version")
    }

    // Check if cache needs updating by comparing local and remote versions
// Corrected version of shouldRefreshCache() in CacheManager.kt
    private var cachedServerVersion: String? = null
    private var serverVersionTimestamp: Long = 0
    private val SERVER_VERSION_CACHE_DURATION = 5 * 60 * 1000 // 5 minutes

    suspend fun shouldRefreshCache(): Boolean {
        val localVersion = getLocalCacheVersion().first()

        try {
            // Check if we have a recent cached server version
            val currentTime = System.currentTimeMillis()
            if (cachedServerVersion != null &&
                (currentTime - serverVersionTimestamp < SERVER_VERSION_CACHE_DURATION)) {
                Log.d(TAG, "Using cached server version: $cachedServerVersion")
                return cachedServerVersion != localVersion
            }

            // Otherwise get from Firestore
            val docRef = firestore.collection(METADATA_COLLECTION).document(CACHE_CONTROL_DOCUMENT)
            val document = docRef.get().await()
            val serverVersion = document.getString(VERSION_FIELD) ?: "0"

            // Cache the result
            cachedServerVersion = serverVersion
            serverVersionTimestamp = currentTime

            Log.d(TAG, "Cache version check - Local: $localVersion, Server: $serverVersion")
            return serverVersion != localVersion

        } catch (e: Exception) {
            Log.e(TAG, "Error checking cache version, assuming refresh needed", e)
            return true // On error, assume we should refresh to be safe
        }
    }    // Setup a listener for cache version changes
    fun listenForCacheVersionChanges(onVersionChange: (String) -> Unit) {
        val docRef = firestore.collection(METADATA_COLLECTION).document(CACHE_CONTROL_DOCUMENT)

        docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening for cache version changes", error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val serverVersion = snapshot.getString(VERSION_FIELD) ?: "0"
                Log.d(TAG, "Cache version changed: $serverVersion")
                onVersionChange(serverVersion)
            }
        }
    }
}