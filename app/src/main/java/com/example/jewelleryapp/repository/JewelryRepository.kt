package com.example.jewelleryapp.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.jewelleryapp.model.Category
import com.example.jewelleryapp.model.Collection
import com.example.jewelleryapp.model.Product
import com.example.jewelleryapp.model.CarouselItem
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

    suspend fun getProductDetails(productId: String): Flow<Product> = flow {
        try {
            val documentSnapshot = firestore.collection("products")
                .document(productId)
                .get()
                .await()

            if (documentSnapshot.exists()) {
                // Get the first image URL from the images array
                val imageUrls = documentSnapshot.get("images") as? List<*>
                val firstImageUrl = imageUrls?.firstOrNull()?.toString() ?: ""
                val httpsImageUrl = storageHelper.getDownloadUrl(firstImageUrl)

                val product = Product(
                    id = documentSnapshot.id,
                    name = documentSnapshot.getString("name") ?: "",
                    price = documentSnapshot.getDouble("price") ?: 0.0,
                    currency = documentSnapshot.getString("currency") ?: "Rs",
                    category_id = documentSnapshot.getString("category_id") ?: "",
                    imageUrl = httpsImageUrl,
                    material_id = documentSnapshot.getString("material_id") ?: "",
                    material_type = documentSnapshot.getString("material_type") ?: "",
                    stone = documentSnapshot.getString("stone") ?: "",
                    clarity = documentSnapshot.getString("clarity") ?: "",
                    cut = documentSnapshot.getString("cut") ?: "",
                    description = documentSnapshot.getString("description") ?: "",
                    isFavorite = false // This will be updated later with user-specific data
                )
                emit(product)
            } else {
                throw Exception("Product not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching product details", e)
            throw e
        }
    }

    suspend fun getProductsByCategory(categoryId: String, excludeProductId: String? = null): Flow<List<Product>> = flow {
        try {
            // Step 1: Get product IDs from category_products
            val categorySnapshot = firestore.collection("category_products")
                .document(categoryId)
                .get()
                .await()

            val productIds = categorySnapshot.get("product_ids") as? List<String> ?: emptyList()

            // Step 2: Query products using whereIn (max 10)
            if (productIds.isNotEmpty()) {
                val snapshot = firestore.collection("products")
                    .whereIn("id", productIds.take(10)) // Firestore limit
                    .get()
                    .await()

                val products = snapshot.documents
                    .filter { it.id != excludeProductId } // Exclude the current product if needed
                    .map { doc ->
                        val imageUrls = doc.get("images") as? List<*>
                        val firstImageUrl = imageUrls?.firstOrNull()?.toString() ?: ""
                        val httpsImageUrl = storageHelper.getDownloadUrl(firstImageUrl)

                        Product(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            price = doc.getDouble("price") ?: 0.0,
                            currency = doc.getString("currency") ?: "Rs",
                            category_id = doc.getString("category_id") ?: "",
                            imageUrl = httpsImageUrl,
                            material = doc.getString("material") ?: "",
                            stone = doc.getString("stone") ?: "",
                            clarity = doc.getString("clarity") ?: "",
                            cut = doc.getString("cut") ?: "",
                            isFavorite = false
                        )
                    }

                Log.d(TAG, "Fetched ${products.size} products for category $categoryId")
                emit(products)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching products by category", e)
            emit(emptyList())
        }
    }


    // Function to toggle wishlist status
    suspend fun toggleWishlist(productId: String, userId: String) {
        try {
            val wishlistRef = firestore.collection("users")
                .document(userId)
                .collection("wishlist")
                .document(productId)

            val doc = wishlistRef.get().await()
            if (doc.exists()) {
                wishlistRef.delete().await()
            } else {
                wishlistRef.set(mapOf(
                    "timestamp" to com.google.firebase.Timestamp.now()
                )).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling wishlist", e)
            throw e
        }
    }

    // Function to check if a product is in wishlist
    suspend fun isInWishlist(productId: String): Boolean {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        return isProductInWishlist(userId, productId)
    }

    // Function to add a product to wishlist
    suspend fun addToWishlist(productId: String): Boolean {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        return addToWishlist(userId, productId)
    }

    // Function to remove a product from wishlist
    suspend fun removeFromWishlist(productId: String): Boolean {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        return removeFromWishlist(userId, productId)
    }
}