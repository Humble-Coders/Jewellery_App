package com.example.jewelleryapp.screen.categoriesScreen

// CategoriesViewModel.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.jewelleryapp.model.Category
import com.example.jewelleryapp.model.Collection
import com.example.jewelleryapp.repository.JewelryRepository

class CategoriesViewModel(private val repository: JewelryRepository) : ViewModel(){
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _collections = MutableStateFlow<List<Collection>>(emptyList())
    val collections: StateFlow<List<Collection>> = _collections

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadCategories()
        loadCollections()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getCategories().collect { categories ->
                _categories.value = categories
                _isLoading.value = false
            }
        }
    }

    private fun loadCollections() {
        viewModelScope.launch {
            repository.getThemedCollections().collect { collections ->
                _collections.value = collections
            }
        }
    }
}