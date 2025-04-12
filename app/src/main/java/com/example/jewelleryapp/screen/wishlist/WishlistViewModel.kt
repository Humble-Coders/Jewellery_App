package com.example.jewelleryapp.screen.wishlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jewelleryapp.model.Category
import com.example.jewelleryapp.model.Product
import com.example.jewelleryapp.repository.CachedJewelryRepository
import com.example.jewelleryapp.repository.JewelryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WishlistViewModel(private val repository: JewelryRepository) : ViewModel() {
    private val TAG = "WishlistViewModel"

    private val _wishlistItems = MutableStateFlow<List<Product>>(emptyList())
    val wishlistItems: StateFlow<List<Product>> = _wishlistItems

    private val _selectedCategory = MutableStateFlow<String>("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _categoryProducts = MutableStateFlow<List<Product>>(emptyList())
    private val _allWishlistItems = MutableStateFlow<List<Product>>(emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        Log.d(TAG, "Initializing WishlistViewModel")
        loadCategories()
        refreshWishlistItems()
    }

    fun refreshWishlistItems() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null  // Clear any previous errors

                Log.d(TAG, "Refreshing wishlist items")

                // First refresh wishlist cache in the repository
                repository.refreshWishlistCache()

                // Then load wishlist items
                loadWishlistItems()

                Log.d(TAG, "Refresh complete")
            } catch (e: Exception) {
                Log.e(TAG, "Error during refresh", e)
                _error.value = "Failed to load wishlist: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadCategoryProducts(categoryId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.getCategoryProducts(categoryId).collect { products ->
                    Log.d(TAG, "Loaded ${products.size} products for category: $categoryId")
                    _categoryProducts.value = products
                    filterWishlistItems()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load category products", e)
                _error.value = "Failed to load category products: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                repository.getCategories().collect { fetchedCategories ->
                    _categories.value = fetchedCategories
                    Log.d(TAG, "Loaded ${fetchedCategories.size} categories")
                }
            } catch (e: Exception) {
                _error.value = "Failed to load categories: ${e.message}"
                Log.e(TAG, "Failed to load categories", e)
            }
        }
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
        if (category != "All") {
            loadCategoryProducts(category)
        } else {
            _categoryProducts.value = emptyList()
            filterWishlistItems()
        }
    }

    private fun loadWishlistItems() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null  // Clear any previous errors

                Log.d(TAG, "Starting to load wishlist items")
                repository.getWishlistItems().collect { items ->
                    Log.d(TAG, "Received ${items.size} wishlist items")

                    if (items.isEmpty()) {
                        Log.d(TAG, "Wishlist is empty")
                    }

                    items.forEach { product ->
                        Log.d(TAG, "Product: id=${product.id}, name=${product.name}, category=${product.category}, imageUrl=${product.imageUrl}")
                    }

                    _allWishlistItems.value = items
                    _wishlistItems.value = items  // Update immediately for All category

                    if (_selectedCategory.value != "All") {
                        filterWishlistItems()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load wishlist items", e)
                _error.value = "Failed to load wishlist items: ${e.message}"
                _wishlistItems.value = emptyList()
                _allWishlistItems.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun filterWishlistItems() {
        val wishlistIds = _allWishlistItems.value.map { it.id }.toSet()
        Log.d(TAG, "Filtering items. Total wishlist items: ${wishlistIds.size}")

        val filteredItems = when (_selectedCategory.value) {
            "All" -> _allWishlistItems.value
            else -> _categoryProducts.value.filter { it.id in wishlistIds }
        }

        Log.d(TAG, "Filtered to ${filteredItems.size} items for category: ${_selectedCategory.value}")
        _wishlistItems.value = filteredItems
    }

    fun removeFromWishlist(productId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "Removing product $productId from wishlist")

                // Call repository to remove from wishlist
                repository.removeFromWishlist(productId)

                // Remove from local lists
                _allWishlistItems.value = _allWishlistItems.value.filter { it.id != productId }
                _wishlistItems.value = _wishlistItems.value.filter { it.id != productId }

                Log.d(TAG, "Successfully removed product from wishlist")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove from wishlist", e)
                _error.value = "Failed to remove from wishlist: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Force refresh all data - useful for pull-to-refresh functionality
    fun forceRefreshData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // If using CachedJewelryRepository, explicitly trigger a refresh
                if (repository is CachedJewelryRepository) {
                    Log.d(TAG, "Explicitly refreshing cache from ViewModel")
                    repository.refreshData()
                }

                // Refresh wishlist cache in repository
                repository.refreshWishlistCache()

                // Reload data
                loadCategories()
                loadWishlistItems()

                // Reload category products if a specific category is selected
                if (_selectedCategory.value != "All") {
                    loadCategoryProducts(_selectedCategory.value)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during manual refresh", e)
                _error.value = "Failed to refresh data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}