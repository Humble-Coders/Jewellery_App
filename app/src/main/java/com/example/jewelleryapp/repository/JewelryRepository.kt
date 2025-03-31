package com.example.jewelleryapp.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.example.jewelleryapp.model.Category
import com.example.jewelleryapp.model.Collection
import com.example.jewelleryapp.model.Product
import com.example.jewelleryapp.model.CarouselItem
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.lang.Exception

class JewelryRepository(
    private val firestore: FirebaseFirestore,
    private val storageHelper: FirebaseStorageHelper
) {
    private val TAG = "JewelryRepository"

    suspend fun getCategories(): Flow<List<Category>> = flow {
        try {
            val snapshot = firestore.collection("categories")
                .orderBy("order")
                .get()
                .await()

            val categories = snapshot.documents.map { doc ->
                val imageUrl = doc.getString("image_url") ?: ""
                val httpsImageUrl = storageHelper.getDownloadUrl(imageUrl)

                Category(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    imageUrl = httpsImageUrl
                )
            }
            Log.d(TAG, "Fetched ${categories.size} categories")
            emit(categories)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching categories", e)
            emit(emptyList<Category>())
        }
    }

    suspend fun getFeaturedProducts(): Flow<List<Product>> = flow {
        try {
            // First get the list of featured product IDs
            val featuredListDoc = firestore.collection("featured_products")
                .document("featured_list")
                .get()
                .await()

            val productIds = featuredListDoc.get("product_ids") as? List<*>

            if (productIds.isNullOrEmpty()) {
                Log.d(TAG, "No featured product IDs found")
                emit(emptyList<Product>())
                return@flow
            }

            Log.d(TAG, "Found ${productIds.size} featured product IDs")

            // Then fetch the actual products
            val products = mutableListOf<Product>()

            // Firebase allows a maximum of 10 items in a whereIn query
            // So we need to batch our requests if we have more than 10 product IDs
            val batches = productIds.chunked(10)

            for (batch in batches) {
                val snapshot = firestore.collection("products")
                    .whereIn("id", batch)
                    .get()
                    .await()

                val batchProducts = snapshot.documents.map { doc ->
                    // Get the original image URL
                    val gsImageUrl = ((doc.get("images") as? List<*>)?.firstOrNull() ?: "").toString()

                    // Convert to HTTPS URL
                    val httpsImageUrl = storageHelper.getDownloadUrl(gsImageUrl)

                    Log.d(TAG, "Product ${doc.getString("name")}: Converting $gsImageUrl to $httpsImageUrl")

                    Product(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        currency = "USD", // Assuming USD as default
                        imageUrl = httpsImageUrl,
                        isFavorite = false // You'd need to implement user-specific wishlist logic here
                    )
                }
                products.addAll(batchProducts)
            }

            Log.d(TAG, "Fetched ${products.size} featured products")
            emit(products)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching featured products", e)
            emit(emptyList<Product>())
        }
    }

    suspend fun getThemedCollections(): Flow<List<Collection>> = flow {
        try {
            val snapshot = firestore.collection("themed_collections")
                .orderBy("order")
                .get()
                .await()

            val collections = snapshot.documents.map { doc ->
                // Get the original image URL
                val gsImageUrl = doc.getString("imageUrl") ?: ""

                // Convert to HTTPS URL
                val httpsImageUrl = storageHelper.getDownloadUrl(gsImageUrl)

                Collection(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    imageUrl = httpsImageUrl,
                    description = doc.getString("description") ?: ""
                )
            }
            Log.d(TAG, "Fetched ${collections.size} themed collections")
            emit(collections)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching themed collections", e)
            emit(emptyList<Collection>())
        }
    }

    suspend fun getCarouselItems(): Flow<List<CarouselItem>> = flow {
        try {
            val snapshot = firestore.collection("carousel_items")
                .get()
                .await()

            val carouselItems = snapshot.documents.map { doc ->
                // Get the original image URL
                val gsImageUrl = doc.getString("imageUrl") ?: ""

                // Convert to HTTPS URL
                val httpsImageUrl = storageHelper.getDownloadUrl(gsImageUrl)

                CarouselItem(
                    id = doc.id,
                    imageUrl = httpsImageUrl,
                    title = doc.getString("title") ?: "",
                    subtitle = doc.getString("subtitle") ?: "",
                    buttonText = doc.getString("buttonText") ?: ""
                )
            }
            Log.d(TAG, "Fetched ${carouselItems.size} carousel items")
            emit(carouselItems)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching carousel items", e)
            emit(emptyList<CarouselItem>())
        }
    }

    // This would be called when a user views a product
    suspend fun recordProductView(userId: String, productId: String) {
        // In a real app, you'd store this in a user's recently viewed collection
        try {
            firestore.collection("users")
                .document(userId)
                .collection("recently_viewed")
                .document(productId)
                .set(mapOf(
                    "timestamp" to com.google.firebase.Timestamp.now()
                ))

            Log.d(TAG, "Recorded product view for user $userId, product $productId")
        } catch (e: Exception) {
            Log.e(TAG, "Error recording product view", e)
        }
    }

    // Function to check if a product is in the user's wishlist
    suspend fun isProductInWishlist(userId: String, productId: String): Boolean {
        return try {
            val wishlistDoc = firestore.collection("wishlists")
                .whereEqualTo("user_id", userId)
                .get()
                .await()

            if (wishlistDoc.documents.isEmpty()) return false

            val wishlistId = wishlistDoc.documents[0].id
            val productIds = wishlistDoc.documents[0].get("product_ids") as? List<String> ?: emptyList()

            productIds.contains(productId)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking wishlist status", e)
            false
        }
    }

    // Function to add a product to the user's wishlist
    suspend fun addToWishlist(userId: String, productId: String): Boolean {
        return try {
            // First check if user has a wishlist
            val wishlistQuery = firestore.collection("wishlists")
                .whereEqualTo("user_id", userId)
                .get()
                .await()

            if (wishlistQuery.documents.isEmpty()) {
                // Create new wishlist
                firestore.collection("wishlists")
                    .add(mapOf(
                        "user_id" to userId,
                        "product_ids" to listOf(productId),
                        "created_at" to com.google.firebase.Timestamp.now()
                    ))
                    .await()
            } else {
                // Update existing wishlist
                val wishlistDoc = wishlistQuery.documents[0]
                val productIds = wishlistDoc.get("product_ids") as? List<String> ?: emptyList()

                if (!productIds.contains(productId)) {
                    firestore.collection("wishlists")
                        .document(wishlistDoc.id)
                        .update("product_ids", productIds + productId)
                        .await()
                }
            }

            Log.d(TAG, "Added product $productId to wishlist for user $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding to wishlist", e)
            false
        }
    }

    // Function to remove a product from the user's wishlist
    suspend fun removeFromWishlist(userId: String, productId: String): Boolean {
        return try {
            val wishlistQuery = firestore.collection("wishlists")
                .whereEqualTo("user_id", userId)
                .get()
                .await()

            if (wishlistQuery.documents.isEmpty()) return false

            val wishlistDoc = wishlistQuery.documents[0]
            val productIds = wishlistDoc.get("product_ids") as? List<String> ?: return false

            if (productIds.contains(productId)) {
                firestore.collection("wishlists")
                    .document(wishlistDoc.id)
                    .update("product_ids", productIds - productId)
                    .await()

                Log.d(TAG, "Removed product $productId from wishlist for user $userId")
                return true
            }

            false
        } catch (e: Exception) {
            Log.e(TAG, "Error removing from wishlist", e)
            false
        }
    }
}