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
    private val userId: String, // Add this parameter
    private val firestore: FirebaseFirestore,
    private val storageHelper: FirebaseStorageHelper
) {
    private val TAG = "JewelryRepository"

    // Function to add test wishlist items if none exist
    suspend fun addTestWishlistItemsIfEmpty() {
        try {
            Log.d(TAG, "Checking if wishlist is empty for user: $userId")
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("wishlist")
                .limit(1)  // Just check if there's at least one item
                .get()
                .await()

            if (snapshot.isEmpty) {
                Log.d(TAG, "Wishlist is empty, adding test items")

                // Use specific product IDs for demo
                val testItems = listOf(
                    "product_8600b6bb",
                    "product_aa2b2cca",
                    "product_b3811b6b"
                )
                Log.d(TAG, "Using demo products: $testItems")

                // Add each test item to the wishlist
                for (productId in testItems) {
                    try {
                        addToWishlist(productId)
                        Log.d(TAG, "Added test item $productId to wishlist")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to add test item $productId", e)
                    }
                }
                Log.d(TAG, "Finished adding test items to wishlist")
            } else {
                Log.d(TAG, "Wishlist already has items, skipping test items")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking/adding test wishlist items", e)
            throw e  // Re-throw to handle in the ViewModel
        }
    }

    // Make sure the getWishlistItems method has proper logging and error handling
    suspend fun getWishlistItems(): Flow<List<Product>> = flow {
        try {
            Log.d(TAG, "Fetching wishlist items for user: $userId")
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("wishlist")
                .get()
                .await()

            val productIds = snapshot.documents.map { it.id }
            Log.d(TAG, "Found ${productIds.size} items in wishlist: $productIds")

            if (productIds.isEmpty()) {
                Log.d(TAG, "No items in wishlist")
                emit(emptyList())
                return@flow
            }

            val products = fetchProductsByIds(productIds)
            Log.d(TAG, "Successfully fetched ${products.size} products for wishlist")

            // Add more detailed logging for debug
            products.forEach { product ->
                Log.d(TAG, "Wishlist product: ${product.id}, ${product.name}, ${product.imageUrl}")
            }

            emit(products)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching wishlist items", e)
            throw e  // Re-throw so it can be caught and handled in the ViewModel
        }
    }
    suspend fun removeFromWishlist(productId: String) {
        try {
            firestore.collection("users")
                .document(userId)
                .collection("wishlist")
                .document(productId)
                .delete()
                .await()
            Log.d(TAG, "Successfully removed product $productId from wishlist")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing product from wishlist", e)
            throw e
        }
    }

    suspend fun addToWishlist(productId: String) {
        try {
            firestore.collection("users")
                .document(userId)
                .collection("wishlist")
                .document(productId)
                .set(mapOf(
                    "addedAt" to System.currentTimeMillis(),
                    "productId" to productId
                ))
                .await()
            Log.d(TAG, "Successfully added product $productId to wishlist")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding product to wishlist", e)
            throw e
        }
    }

/*

    suspend fun getWishlistItems(): Flow<List<Product>> = flow {
        try {
            Log.d(TAG, "Fetching wishlist items for user: $userId")
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("wishlist")
                .get()
                .await()

            val productIds = snapshot.documents.map { it.id }
            Log.d(TAG, "Found ${productIds.size} items in wishlist: $productIds")

            if (productIds.isEmpty()) {
                Log.d(TAG, "No items in wishlist")
                emit(emptyList())
                return@flow
            }

            val products = fetchProductsByIds(productIds)
            Log.d(TAG, "Successfully fetched ${products.size} products for wishlist")
            emit(products)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching wishlist items for user: $userId", e)
            emit(emptyList())
        }
    }
*/
    suspend fun getCategoryProducts(categoryId: String): Flow<List<Product>> = flow {
        try {
            // Get product IDs from category_products collection
            val categoryDoc = firestore.collection("category_products")
                .document("category_${categoryId.lowercase()}")
                .get()
                .await()

            val productIds = (categoryDoc.get("product_ids") as? List<*>)?.map { it.toString() } ?: emptyList()

            if (productIds.isEmpty()) {
                emit(emptyList())
                return@flow
            }

            val products = fetchProductsByIds(productIds)
            emit(products)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching category products", e)
            emit(emptyList())
        }
    }

    private suspend fun fetchProductsByIds(productIds: List<String>): List<Product> {
        val products = mutableListOf<Product>()

        try {
            Log.d(TAG, "Fetching products for IDs: $productIds")
            // Fetch products in batches of 10 (Firestore limitation)
            productIds.chunked(10).forEach { batch ->
                Log.d(TAG, "Fetching batch of products: $batch")
                // Get each product document directly by its ID
                val batchProducts = batch.mapNotNull { productId ->
                    try {
                        val doc = firestore.collection("products").document(productId).get().await()
                        if (!doc.exists()) {
                            Log.e(TAG, "Product document $productId does not exist")
                            null
                        } else {
                            doc
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching product $productId", e)
                        null
                    }
                }.map { doc ->
                        // Get the image URL from the images array
                        val images = doc.get("images") as? List<*>
                        val imageUrl = when {
                            images.isNullOrEmpty() -> ""
                            images[0] is String -> images[0] as String
                            else -> ""
                        }
                        Log.d(TAG, "Raw image URL for product ${doc.id}: $imageUrl")
                        
                        // Convert to HTTPS URL if it's a valid image path
                        val httpsImageUrl = if (imageUrl.isNotEmpty()) {
                            storageHelper.getDownloadUrl(imageUrl)
                        } else {
                            ""
                        }
                        Log.d(TAG, "Converted image URL for product ${doc.id}: $httpsImageUrl")

                        Log.d(TAG, "Processing product: ${doc.id}, name: ${doc.getString("name")}, type: ${doc.getString("type")}")

                        Product(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            price = doc.getDouble("price") ?: 0.0,
                            currency = "Rs",
                            imageUrl = httpsImageUrl,
                            isFavorite = true,
                            category = doc.getString("type") ?: ""
                        )
                    }
                products.addAll(batchProducts)
                Log.d(TAG, "Added ${batchProducts.size} products from batch")
            }
            Log.d(TAG, "Successfully fetched ${products.size} products")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching products by IDs: $productIds", e)
            throw e  // Re-throw to handle in ViewModel
        }

        return products
    }



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
                        currency = "Rs", // Using Rs as default currency
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