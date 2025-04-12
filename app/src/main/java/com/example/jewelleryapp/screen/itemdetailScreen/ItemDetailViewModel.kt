package com.example.jewelleryapp.screen.itemdetailScreen

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jewelleryapp.model.Product
import com.example.jewelleryapp.repository.CachedJewelryRepository
import com.example.jewelleryapp.repository.JewelryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ItemDetailViewModel(
    private val repository: JewelryRepository
) : ViewModel() {

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product.asStateFlow()

    private val _similarProducts = MutableStateFlow<List<Product>>(emptyList())
    val similarProducts = _similarProducts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isInWishlist = MutableStateFlow(false)
    val isInWishlist: StateFlow<Boolean> = _isInWishlist.asStateFlow()

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Load product details using Flow
                repository.getProductDetails(productId).collect { productDetails ->
                    _product.value = productDetails
                    Log.d(TAG, "Loaded product with material_id: ${productDetails.material_id}, material_type: ${productDetails.material_type}")
                }

                // Check wishlist status
                checkWishlistStatus(productId)

            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred while loading the product"
                Log.e(TAG, "Error loading product details", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun checkWishlistStatus(productId: String) {
        viewModelScope.launch {
            try {
                val isInWishlistResult = repository.isInWishlist(productId)
                _isInWishlist.value = isInWishlistResult
                Log.d(TAG, "Product $productId wishlist status: $isInWishlistResult")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking wishlist status", e)
                // Don't update the wishlist status on error
            }
        }
    }

    fun toggleWishlist() {
        viewModelScope.launch {
            try {
                val currentProduct = _product.value ?: return@launch
                val currentWishlistStatus = _isInWishlist.value

                // Toggle loading state
                _isLoading.value = true

                if (currentWishlistStatus) {
                    // If currently in wishlist, remove it
                    repository.removeFromWishlist(currentProduct.id)
                    _isInWishlist.value = false
                    Log.d(TAG, "Successfully removed product ${currentProduct.id} from wishlist")
                } else {
                    // If not in wishlist, add it
                    repository.addToWishlist(currentProduct.id)
                    _isInWishlist.value = true
                    Log.d(TAG, "Successfully added product ${currentProduct.id} to wishlist")
                }

                // Update similar products if the toggled product is there
                updateSimilarProductWishlistStatus(currentProduct.id, !currentWishlistStatus)
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling wishlist", e)
                _error.value = e.message ?: "An error occurred while updating wishlist"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Add this method to ItemDetailViewModel.kt
    fun toggleSimilarProductWishlist(productId: String) {
        viewModelScope.launch {
            try {
                // Find the product in the similar products list
                val similarProduct = _similarProducts.value.find { it.id == productId }
                if (similarProduct != null) {
                    val currentWishlistStatus = similarProduct.isFavorite

                    _isLoading.value = true

                    if (currentWishlistStatus) {
                        // If currently in wishlist, remove it
                        repository.removeFromWishlist(productId)

                        // Update similar products list with new wishlist status
                        updateSimilarProductWishlistStatus(productId, false)
                        Log.d(TAG, "Successfully removed similar product $productId from wishlist")
                    } else {
                        // If not in wishlist, add it
                        repository.addToWishlist(productId)

                        // Update similar products list with new wishlist status
                        updateSimilarProductWishlistStatus(productId, true)
                        Log.d(TAG, "Successfully added similar product $productId to wishlist")
                    }

                    // If the toggled product is the current product, update its status too
                    if (productId == _product.value?.id) {
                        _isInWishlist.value = !currentWishlistStatus
                    }
                } else {
                    Log.e(TAG, "Similar product $productId not found in similar products list")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling wishlist for similar product", e)
                _error.value = e.message ?: "An error occurred while updating wishlist"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updateSimilarProductWishlistStatus(productId: String, isFavorite: Boolean) {
        _similarProducts.value = _similarProducts.value.map {
            if (it.id == productId) it.copy(isFavorite = isFavorite) else it
        }
    }

    fun loadSimilarProducts() {
        val currentProduct = product.value ?: return
        viewModelScope.launch {
            try {
                // Log before fetching
                Log.d(TAG, "Loading similar products for category: ${currentProduct.category_id}")

                // Collect the flow from the repository
                repository.getProductsByCategory(
                    categoryId = currentProduct.category_id,
                    excludeProductId = currentProduct.id
                ).collect { similarProductsList ->
                    // Check wishlist status for similar products
                    val productsWithWishlistStatus = similarProductsList.map { product ->
                        try {
                            val isInWishlist = repository.isInWishlist(product.id)
                            product.copy(isFavorite = isInWishlist)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error checking wishlist status for similar product ${product.id}", e)
                            product
                        }
                    }

                    // Update the similar products state
                    _similarProducts.value = productsWithWishlistStatus

                    // Log after fetching
                    Log.d(TAG, "Loaded ${similarProductsList.size} similar products")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading similar products", e)
                _similarProducts.value = emptyList()
            }
        }
    }

    // Force refresh product data - useful if cache is stale
    fun refreshProductData(productId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // If using CachedJewelryRepository, explicitly trigger a refresh
                if (repository is CachedJewelryRepository) {
                    Log.d(TAG, "Explicitly refreshing cache from ViewModel")
                    repository.refreshData()
                }

                // Reload the product
                loadProduct(productId)

                // Reload similar products if we have a current product
                product.value?.let {
                    loadSimilarProducts()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during product refresh", e)
                _error.value = "Failed to refresh product: ${e.message}"
                _isLoading.value = false
            }
        }
    }
}