package com.vicky.recsdk.demo.ui.detail

import androidx.lifecycle.ViewModel
import com.vicky.recsdk.RecoEngine
import com.vicky.recsdk.demo.data.model.DummyProduct
import com.vicky.recsdk.model.EventType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProductDetailViewModel : ViewModel() {

    private val _product = MutableStateFlow<DummyProduct?>(null)
    val product: StateFlow<DummyProduct?> = _product.asStateFlow()

    fun setProduct(product: DummyProduct) {
        _product.value = product
        // Track VIEW event when product detail is opened
        RecoEngine.trackEvent(EventType.VIEW, product.id.toString())
    }

    fun onAddToCart() {
        val p = _product.value ?: return
        RecoEngine.trackEvent(EventType.ADD_TO_CART, p.id.toString())
    }

    fun onFavorite() {
        val p = _product.value ?: return
        RecoEngine.trackEvent(EventType.FAVORITE, p.id.toString())
    }

    fun onPurchase() {
        val p = _product.value ?: return
        RecoEngine.trackEvent(EventType.PURCHASE, p.id.toString())
    }
}
