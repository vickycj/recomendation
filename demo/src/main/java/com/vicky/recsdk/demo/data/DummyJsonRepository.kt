package com.vicky.recsdk.demo.data

import com.vicky.recsdk.demo.data.model.DummyProduct

class DummyJsonRepository(
    private val apiService: DummyJsonApiService = RetrofitClient.apiService
) {

    suspend fun fetchAllProducts(): List<DummyProduct> {
        return apiService.getProducts(limit = 100).products
    }

    suspend fun fetchCategories(): List<String> {
        return apiService.getCategories().map { it.name }
    }
}
