package com.example.jewelleryapp.screen.homeScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jewelleryapp.model.CarouselItem
import com.example.jewelleryapp.model.Category
import com.example.jewelleryapp.model.Collection
import com.example.jewelleryapp.model.Product
import com.example.jewelleryapp.repository.JewelryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: JewelryRepository) : ViewModel() {

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
                    _featuredProducts.value = products
                }

                // Load themed collections
                repository.getThemedCollections().collect { collections ->
                    _collections.value = collections
                }

                // Load carousel items
                repository.getCarouselItems().collect { items ->
                    _carouselItems.value = items
                }

                // Removing recently viewed products loading as requested
                // We'll use collections instead

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Failed to load data: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Function to refresh all data
    fun refreshData() {
        loadData()
    }

    // Called when a product is viewed
    fun recordProductView(userId: String, productId: String) {
        viewModelScope.launch {
            try {
                repository.recordProductView(userId, productId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Toggle favorite status
    fun toggleFavorite(productId: String) {
        // Update featured products
        _featuredProducts.value = _featuredProducts.value.map { product ->
            if (product.id == productId) {
                product.copy(isFavorite = !product.isFavorite)
            } else {
                product
            }
        }

        // Update recently viewed products
        _recentlyViewedProducts.value = _recentlyViewedProducts.value.map { product ->
            if (product.id == productId) {
                product.copy(isFavorite = !product.isFavorite)
            } else {
                product
            }
        }

        // In a real app, you would also update this in Firebase
    }
}