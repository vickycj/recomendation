package com.vicky.recsdk.demo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vicky.recsdk.RecoEngine
import com.vicky.recsdk.demo.data.DummyJsonRepository
import com.vicky.recsdk.demo.data.model.DummyProduct
import com.vicky.recsdk.demo.data.model.ProductMapper
import com.vicky.recsdk.model.EventType
import com.vicky.recsdk.model.RecoResult
import com.vicky.recsdk.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: DummyJsonRepository = DummyJsonRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val products = repository.fetchAllProducts()
                val categories = products.map { it.category }.distinct().sorted()

                // Feed items to RecoEngine
                val recoItems = ProductMapper.toRecoItems(products)
                RecoEngine.feedItems(recoItems)

                // Get recommendations
                val recommendations = RecoEngine.getRecommendations()
                val profile = RecoEngine.getUserProfile()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    products = products,
                    categories = categories,
                    recommendations = recommendations,
                    userProfile = profile,
                    selectedCategory = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load products"
                )
            }
        }
    }

    fun onProductClicked(product: DummyProduct) {
        RecoEngine.trackEvent(EventType.CLICK, product.id.toString())
        refreshRecommendations()
    }

    fun onProductAddedToCart(product: DummyProduct) {
        RecoEngine.trackEvent(EventType.ADD_TO_CART, product.id.toString())
        refreshRecommendations()
    }

    fun onCategorySelected(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun onClearData() {
        RecoEngine.clearUserData()
        refreshRecommendations()
    }

    private fun refreshRecommendations() {
        viewModelScope.launch {
            try {
                val recommendations = RecoEngine.getRecommendations()
                val profile = RecoEngine.getUserProfile()
                _uiState.value = _uiState.value.copy(
                    recommendations = recommendations,
                    userProfile = profile
                )
            } catch (e: Exception) {
                // Silently handle refresh errors
            }
        }
    }
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val products: List<DummyProduct> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val recommendations: RecoResult = RecoResult(),
    val userProfile: UserProfile = UserProfile()
) {
    val filteredProducts: List<DummyProduct>
        get() = if (selectedCategory != null) {
            products.filter { it.category == selectedCategory }
        } else {
            products
        }
}
