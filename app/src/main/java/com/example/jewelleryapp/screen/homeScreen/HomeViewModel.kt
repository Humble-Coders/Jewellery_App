package com.example.jewelleryapp.screen.homeScreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jewelleryapp.model.CarouselItem
import com.example.jewelleryapp.model.Category
import com.example.jewelleryapp.model.Collection
import com.example.jewelleryapp.model.Product
import com.example.jewelleryapp.repository.CachedJewelryRepository
import com.example.jewelleryapp.repository.JewelryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: JewelryRepository) : ViewModel() {
    private val TAG = "HomeViewModel"

    // StateFlows to hold data for UI
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _featuredProducts = MutableStateFlow<List<Product>>(emptyList())
    val featuredProducts: StateFlow<List<Product>> = _featuredProducts.asStateFlow()

    private val _collections = MutableStateFlow<List<Collection>>(emptyList())
    val collections: StateFlow<List<Collection>> = _collections.asStateFlow()

    private val _carouselItems = MutableStateFlow<List<CarouselItem>>(emptyList())
    val carouselItems: StateFlow<List<CarouselItem>> = _carouselItems.asStateFlow()

    private val _recentlyViewedProducts = MutableStateFlow<List<Product>>(emptyList())
    val recentlyViewedProducts: StateFlow<List<Product>> = _recentlyViewedProducts.asStateFlow()

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Initialize by loading all data
    init {
        loadData()
    }

    fun loadData() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // Load categories
                repository.getCategories().collect { categories ->
                    _categories.value = categories
                }

                // Load featured products
                repository.getFeaturedProducts().collect { products ->
                    // Update wishlist status for all products
                    updateProductsWithWishlistStatus(products)
                }

                // Load themed collections
                repository.getThemedCollections().collect { collections ->
                    _collections.value = collections
                }

                // Load carousel items
                repository.getCarouselItems().collect { items ->
                    _carouselItems.value = items
                }

                _isLoading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load data", e)
                _error.value = "Failed to load data: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Function to refresh all data - forces a refresh from the repository
    fun refreshData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // If using CachedJewelryRepository, explicitly trigger a refresh
                if (repository is CachedJewelryRepository) {
                    Log.d(TAG, "Explicitly refreshing cache from ViewModel")
                    repository.refreshData()
                }

                // Load data as usual after refresh
                loadData()
            } catch (e: Exception) {
                Log.e(TAG, "Error during manual refresh", e)
                _error.value = "Failed to refresh data: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Called when a product is viewed
    fun recordProductView(userId: String, productId: String) {
        viewModelScope.launch {
            try {
                repository.recordProductView(userId, productId)
            } catch (e: Exception) {
                Log.e(TAG, "Error recording product view", e)
            }
        }
    }

    // New function to update all products with current wishlist status
    private suspend fun updateProductsWithWishlistStatus(products: List<Product>) {
        try {
            val productsWithWishlistStatus = products.map { product ->
                try {
                    val isInWishlist = repository.isInWishlist(product.id)
                    product.copy(isFavorite = isInWishlist)
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking wishlist status for product ${product.id}", e)
                    product
                }
            }
            _featuredProducts.value = productsWithWishlistStatus
            Log.d(TAG, "Updated wishlist status for ${products.size} products")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating wishlist status for products", e)
        }
    }

    // Check if a product is in wishlist
    fun checkWishlistStatus(productId: String) {
        viewModelScope.launch {
            try {
                val isInWishlist = repository.isInWishlist(productId)

                // Update the featured products list with the current wishlist status
                _featuredProducts.value = _featuredProducts.value.map { product ->
                    if (product.id == productId) {
                        product.copy(isFavorite = isInWishlist)
                    } else {
                        product
                    }
                }

                // Also update recently viewed products if needed
                _recentlyViewedProducts.value = _recentlyViewedProducts.value.map { product ->
                    if (product.id == productId) {
                        product.copy(isFavorite = isInWishlist)
                    } else {
                        product
                    }
                }

                Log.d(TAG, "Product $productId wishlist status: $isInWishlist")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking wishlist status for product $productId", e)
            }
        }
    }

    // Toggle favorite status and update in repository
    fun toggleFavorite(productId: String) {
        viewModelScope.launch {
            try {
                // Get current favorite status from our list
                val currentProduct = _featuredProducts.value.find { it.id == productId }
                    ?: _recentlyViewedProducts.value.find { it.id == productId }

                if (currentProduct != null) {
                    val isCurrentlyFavorite = currentProduct.isFavorite

                    // Toggle in repository
                    if (isCurrentlyFavorite) {
                        repository.removeFromWishlist(productId)
                    } else {
                        repository.addToWishlist(productId)
                    }

                    // Update UI state
                    updateProductFavoriteStatus(productId, !isCurrentlyFavorite)

                    Log.d(TAG, "Toggled favorite for product $productId to ${!isCurrentlyFavorite}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling favorite for product $productId", e)
            }
        }
    }

    // Helper function to update favorite status in state
    private fun updateProductFavoriteStatus(productId: String, isFavorite: Boolean) {
        // Update featured products
        _featuredProducts.value = _featuredProducts.value.map { product ->
            if (product.id == productId) {
                product.copy(isFavorite = isFavorite)
            } else {
                product
            }
        }

        // Update recently viewed products
        _recentlyViewedProducts.value = _recentlyViewedProducts.value.map { product ->
            if (product.id == productId) {
                product.copy(isFavorite = isFavorite)
            } else {
                product
            }
        }
    }

    suspend fun loadCachedDataSync(): Boolean {
        try {
            if (repository is CachedJewelryRepository && repository.hasCachedData()) {
                _categories.value = repository.getCategoriesSync()

                // Get featured products with wishlist status
                val products = repository.getFeaturedProductsSync()
                _featuredProducts.value = products

                _collections.value = repository.getCollectionsSync()
                _carouselItems.value = repository.getCarouselItemsSync()

                return true
            }
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cached data synchronously", e)
            return false
        }
    }

    // Check for updates without immediately changing UI
    fun checkForUpdates() {
        viewModelScope.launch {
            try {
                if (repository is CachedJewelryRepository) {
                    val shouldRefresh = repository.shouldRefreshCacheSync()
                    if (shouldRefresh) {
                        _isLoading.value = true
                        repository.refreshData()
                        loadData() // Reload from updated cache
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for updates", e)
            }
        }
    }
}