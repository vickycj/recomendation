package com.vicky.recsdk.demo.data.model

import com.google.gson.annotations.SerializedName

data class DummyProductResponse(
    val products: List<DummyProduct>,
    val total: Int,
    val skip: Int,
    val limit: Int
)

data class DummyProduct(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val price: Double,
    @SerializedName("discountPercentage") val discountPercentage: Double = 0.0,
    val rating: Double = 0.0,
    val stock: Int = 0,
    val tags: List<String> = emptyList(),
    val brand: String? = null,
    val sku: String = "",
    val thumbnail: String = "",
    val images: List<String> = emptyList()
)
