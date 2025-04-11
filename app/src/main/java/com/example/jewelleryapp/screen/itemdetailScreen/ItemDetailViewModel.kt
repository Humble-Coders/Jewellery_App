package com.example.jewelleryapp.screen.itemdetailScreen

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jewelleryapp.model.Product
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

                _isInWishlist.value = repository.isInWishlist(productId)

            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred while loading the product"
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun toggleWishlist() {
        viewModelScope.launch {
            try {
                val currentProduct = _product.value ?: return@launch
                val currentWishlistStatus = _isInWishlist.value

                if (currentWishlistStatus) {
                    repository.removeFromWishlist(currentProduct.id)
                } else {
                    repository.addToWishlist(currentProduct.id)
                }

                _isInWishlist.value = !currentWishlistStatus

            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred while updating wishlist"
            }
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
                    // Update the similar products state
                    _similarProducts.value = similarProductsList

                    // Log after fetching
                    Log.d(TAG, "Loaded ${similarProductsList.size} similar products")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading similar products", e)
                _similarProducts.value = emptyList()
            }
        }
    }

}