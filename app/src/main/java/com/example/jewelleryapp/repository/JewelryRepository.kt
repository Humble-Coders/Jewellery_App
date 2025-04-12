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

open class JewelryRepository(
    private val userId: String, // Add this parameter
    private val firestore: FirebaseFirestore,
    private val storageHelper: FirebaseStorageHelper
) {
    private val TAG = "JewelryRepository"

    // Cache for wishlist status to avoid excessive Firestore calls
    private val wishlistCache = mutableMapOf<String, Boolean>()

    // Function to add test wishlist items if none exist

    // Function to update wishlist cache from Firestore
    suspend fun refreshWishlistCache() {
        try {
            Log.d(TAG, "Refreshing wishlist cache for user: $userId")
            if (userId.isBlank()) {
                Log.d(TAG, "User ID is blank, cannot refresh wishlist cache")
                wishlistCache.clear()
                return
            }

            // Clear existing cache
            wishlistCache.clear()

            // Get all wishlist items
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("wishlist")
                .get()
                .await()

            // Update cache with all items
            snapshot.documents.forEach { doc ->
                wishlistCache[doc.id] = true
            }

            Log.d(TAG, "Refreshed wishlist cache with ${wishlistCache.size} items")
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing wishlist cache", e)
            // Don't clear cache on error to prevent data loss
        }
    }

    // Make sure the getWishlistItems method has proper logging and error handling
    suspend fun getWishlistItems(): Flow<List<Product>> = flow {
        try {
            Log.d(TAG, "Fetching wishlist items for user: $userId")

            // Refresh the cache first
            refreshWishlistCache()

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

            // Mark all products as favorites since they're in the wishlist
            val productsWithFavoriteFlag = products.map { it.copy(isFavorite = true) }
            emit(productsWithFavoriteFlag)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching wishlist items", e)
            throw e  // Re-throw so it can be caught and handled in the ViewModel
        }
    }

    open suspend fun removeFromWishlist(productId: String) {
        try {
            if (userId.isBlank()) {
                Log.e(TAG, "Cannot remove from wishlist: User ID is blank")
                return
            }

            firestore.collection("users")
                .document(userId)
                .collection("wishlist")
                .document(productId)
                .delete()
                .await()

            // Update cache
            wishlistCache[productId] = false
            Log.d(TAG, "Successfully removed product $productId from wishlist")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing product from wishlist", e)
            throw e
        }
    }

    open suspend fun addToWishlist(productId: String) {
        try {
            if (userId.isBlank()) {
                Log.e(TAG, "Cannot add to wishlist: User ID is blank")
                return
            }

            firestore.collection("users")
                .document(userId)
                .collection("wishlist")
                .document(productId)
                .set(mapOf(
                    "addedAt" to System.currentTimeMillis(),
                    "productId" to productId
                ))
                .await()

            // Update cache
            wishlistCache[productId] = true
            Log.d(TAG, "Successfully added product $productId to wishlist")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding product to wishlist", e)
            throw e
        }
    }

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

                    // Check wishlist status from cache
                    val isFavorite = wishlistCache[doc.id] == true

                    Product(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        currency = "Rs",
                        imageUrl = httpsImageUrl,
                        isFavorite = isFavorite,
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

    open suspend fun getCategories(): Flow<List<Category>> = flow {
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

    open suspend fun getFeaturedProducts(): Flow<List<Product>> = flow {
        try {
            // Make sure the wishlist cache is up to date
            refreshWishlistCache()

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

                    // Check wishlist status from cache
                    val productId = doc.getString("id") ?: doc.id
                    val isFavorite = wishlistCache[productId] == true

                    Product(
                        id = productId,
                        name = doc.getString("name") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        currency = "Rs", // Using Rs as default currency
                        imageUrl = httpsImageUrl,
                        isFavorite = isFavorite
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

    open suspend fun getThemedCollections(): Flow<List<Collection>> = flow {
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

    open suspend fun getCarouselItems(): Flow<List<CarouselItem>> = flow {
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
    suspend fun isInWishlist(productId: String): Boolean {
        try {
            // Check the cache first if it exists
            if (wishlistCache.containsKey(productId)) {
                val cachedStatus = wishlistCache[productId] == true
                Log.d(TAG, "Found product $productId in wishlist cache: $cachedStatus")
                return cachedStatus
            }

            // If not in cache, check from Firestore
            if (userId.isBlank()) {
                Log.d(TAG, "User ID is blank, cannot check wishlist status")
                return false
            }

            val doc = firestore.collection("users")
                .document(userId)
                .collection("wishlist")
                .document(productId)
                .get()
                .await()

            val exists = doc.exists()

            // Update the cache
            wishlistCache[productId] = exists

            Log.d(TAG, "Checked Firestore for product $productId in wishlist: $exists")
            return exists
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if product is in wishlist", e)
            return false
        }
    }

    suspend fun getProductDetails(productId: String): Flow<Product> = flow {
        try {
            // Make sure wishlist cache is up to date
            if (wishlistCache.isEmpty()) {
                refreshWishlistCache()
            }

            val documentSnapshot = firestore.collection("products")
                .document(productId)
                .get()
                .await()

            if (documentSnapshot.exists()) {
                // Get the first image URL from the images array
                val imageUrls = documentSnapshot.get("images") as? List<*>
                val firstImageUrl = imageUrls?.firstOrNull()?.toString() ?: ""
                val httpsImageUrl = storageHelper.getDownloadUrl(firstImageUrl)

                // Check wishlist status
                val isInWishlist = wishlistCache[productId] == true

                val product = Product(
                    id = documentSnapshot.id,
                    name = documentSnapshot.getString("name") ?: "",
                    price = documentSnapshot.getDouble("price") ?: 0.0,
                    currency = documentSnapshot.getString("currency") ?: "Rs",
                    category_id = documentSnapshot.getString("category_id") ?: "",
                    imageUrl = httpsImageUrl,
                    material_id = documentSnapshot.getString("material_id"),
                    material_type = documentSnapshot.getString("material_type"),
                    stone = documentSnapshot.getString("stone") ?: "",
                    clarity = documentSnapshot.getString("clarity") ?: "",
                    cut = documentSnapshot.getString("cut") ?: "",
                    description = documentSnapshot.getString("description") ?: "",
                    isFavorite = isInWishlist
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
            // Refresh wishlist cache first
            refreshWishlistCache()

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

                        // Check wishlist status from cache
                        val isInWishlist = wishlistCache[doc.id] == true

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
                            isFavorite = isInWishlist
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
    }}