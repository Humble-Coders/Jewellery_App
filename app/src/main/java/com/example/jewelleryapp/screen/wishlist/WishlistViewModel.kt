package com.example.jewelleryapp.screen.wishlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jewelleryapp.model.Category
import com.example.jewelleryapp.model.Product
import com.example.jewelleryapp.repository.JewelryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WishlistViewModel(private val repository: JewelryRepository) : ViewModel() {
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
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null  // Clear any previous errors
                Log.d("WishlistViewModel", "Initializing WishlistViewModel")

                // First add test items if wishlist is empty
                try {
                    repository.addTestWishlistItemsIfEmpty()
                    Log.d("WishlistViewModel", "Test items added successfully if needed")
                } catch (e: Exception) {
                    Log.e("WishlistViewModel", "Failed to add test items", e)
                    // Continue anyway - we'll try to load whatever's in the wishlist
                }

                // Then load categories and wishlist items
                loadCategories()
                loadWishlistItems()

                Log.d("WishlistViewModel", "Initialization complete")
            } catch (e: Exception) {
                Log.e("WishlistViewModel", "Error during initialization", e)
                _error.value = "Failed to initialize: ${e.message}"
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
                    Log.d("WishlistViewModel", "Loaded ${products.size} products for category: $categoryId")
                    _categoryProducts.value = products
                    filterWishlistItems()
                }
            } catch (e: Exception) {
                Log.e("WishlistViewModel", "Failed to load category products", e)
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
                }
            } catch (e: Exception) {
                _error.value = "Failed to load categories: ${e.message}"
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

                Log.d("WishlistViewModel", "Starting to load wishlist items")
                repository.getWishlistItems().collect { items ->
                    Log.d("WishlistViewModel", "Received ${items.size} wishlist items")

                    if (items.isEmpty()) {
                        Log.d("WishlistViewModel", "Wishlist is empty")
                    }

                    items.forEach { product ->
                        Log.d("WishlistViewModel", "Product: id=${product.id}, name=${product.name}, category=${product.category}, imageUrl=${product.imageUrl}")
                    }

                    _allWishlistItems.value = items
                    _wishlistItems.value = items  // Update immediately for All category

                    if (_selectedCategory.value != "All") {
                        filterWishlistItems()
                    }
                }
            } catch (e: Exception) {
                Log.e("WishlistViewModel", "Failed to load wishlist items", e)
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
        Log.d("WishlistViewModel", "Filtering items. Total wishlist items: ${wishlistIds.size}")

        val filteredItems = when (_selectedCategory.value) {
            "All" -> _allWishlistItems.value
            else -> _categoryProducts.value.filter { it.id in wishlistIds }
        }

        Log.d("WishlistViewModel", "Filtered to ${filteredItems.size} items for category: ${_selectedCategory.value}")
        _wishlistItems.value = filteredItems
    }
    fun removeFromWishlist(productId: String) {
        viewModelScope.launch {
            try {
                repository.removeFromWishlist(productId)
                // Remove from sample data
                _wishlistItems.value = _wishlistItems.value.filter { it.id != productId }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
