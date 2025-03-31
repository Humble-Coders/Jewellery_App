package com.example.jewelleryapp.repository

import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await

/**
 * Helper class for Firebase Storage operations
 * Primarily converts gs:// URLs to HTTPS download URLs
 */
class FirebaseStorageHelper {
    private val TAG = "FirebaseStorageHelper"
    private val storage = Firebase.storage

    // Cache of previously converted URLs to avoid redundant conversions
    private val urlCache = mutableMapOf<String, String>()

    /**
     * Converts a gs:// URL to an HTTPS download URL
     *
     * @param gsUrl The gs:// URL to convert (e.g., gs://bucket-name/path/to/image.jpg)
     * @return The HTTPS download URL or the original URL if conversion fails
     */
    suspend fun getDownloadUrl(gsUrl: String): String {
        // Return empty string for empty input
        if (gsUrl.isEmpty()) return ""

        // If not a gs:// URL or already an HTTPS URL, return as is
        if (!gsUrl.startsWith("gs://")) return gsUrl

        // Check cache first
        if (urlCache.containsKey(gsUrl)) {
            return urlCache[gsUrl]!!
        }

        return try {
            // Extract the path from the gs:// URL
            val path = gsUrl.removePrefix("gs://")

            // Find the bucket name and path
            val separatorIndex = path.indexOf('/')

            if (separatorIndex == -1) {
                Log.e(TAG, "Invalid gs:// URL format: $gsUrl")
                return gsUrl
            }

            val bucketName = path.substring(0, separatorIndex)
            val storagePath = path.substring(separatorIndex + 1)

            // Get storage instance for the specific bucket
            val storageInstance = Firebase.storage("gs://$bucketName")
            val storageRef = storageInstance.reference.child(storagePath)

            // Get download URL
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // Cache the result
            urlCache[gsUrl] = downloadUrl

            Log.d(TAG, "Converted $gsUrl to $downloadUrl")
            downloadUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error converting gs:// URL: $gsUrl", e)
            // Return original URL if conversion fails
            gsUrl
        }
    }

    /**
     * Clears the URL cache
     */
    fun clearCache() {
        urlCache.clear()
    }
}