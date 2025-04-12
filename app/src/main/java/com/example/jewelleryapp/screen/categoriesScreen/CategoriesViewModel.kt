package com.example.jewelleryapp.screen.categoriesScreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.jewelleryapp.model.Category
import com.example.jewelleryapp.model.Collection
import com.example.jewelleryapp.repository.CachedJewelryRepository
import com.example.jewelleryapp.repository.JewelryRepository

class CategoriesViewModel(private val repository: JewelryRepository) : ViewModel() {
    private val TAG = "CategoriesViewModel"

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _collections = MutableStateFlow<List<Collection>>(emptyList())
    val collections: StateFlow<List<Collection>> = _collections

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadCategories()
        loadCollections()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                repository.getCategories().collect { categories ->
                    _categories.value = categories
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading categories", e)
                _error.value = "Failed to load categories: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private fun loadCollections() {
        viewModelScope.launch {
            try {
                repository.getThemedCollections().collect { collections ->
                    _collections.value = collections
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading collections", e)
                // Don't update error state here, as we still want to show categories
                // even if collections fail to load
            }
        }
    }

    // Force refresh data - useful for pull-to-refresh functionality
    fun refreshData() {
        viewModelScope.launch {
            try {
                // If using CachedJewelryRepository, explicitly trigger a refresh
                if (repository is CachedJewelryRepository) {
                    Log.d(TAG, "Explicitly refreshing cache from ViewModel")
                    repository.refreshData()
                }

                // Re-load data
                loadCategories()
                loadCollections()
            } catch (e: Exception) {
                Log.e(TAG, "Error during manual refresh", e)
                _error.value = "Failed to refresh data: ${e.message}"
            }
        }
    }

    suspend fun loadCachedDataSync(): Boolean {
        try {
            if (repository is CachedJewelryRepository) {
                // Check if we have cached data
                val cachedCategories = (repository.categoriesCache.value)
                val cachedCollections = (repository.collectionsCache.value)

                // Only consider cache valid if categories and collections exist
                val hasCachedData = cachedCategories.isNotEmpty()

                if (hasCachedData) {
                    // Update StateFlows with cached data
                    _categories.value = cachedCategories
                    _collections.value = cachedCollections
                    return true
                }
            }
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cached data synchronously", e)
            return false
        }
    }

    // Add this method to check for updates
    fun checkForUpdates() {
        viewModelScope.launch {
            try {
                if (repository is CachedJewelryRepository) {
                    val cacheManager = (repository as CachedJewelryRepository).cacheManager
                    val shouldRefresh = cacheManager.shouldRefreshCache()
                    if (shouldRefresh) {
                        _isLoading.value = true
                        (repository as CachedJewelryRepository).refreshData()
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for updates", e)
            }
        }
    }
}