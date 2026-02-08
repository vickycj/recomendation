package com.vicky.recsdk.demo.data

import com.vicky.recsdk.demo.data.model.DummyProductResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface DummyJsonApiService {

    @GET("products")
    suspend fun getProducts(
        @Query("limit") limit: Int = 100,
        @Query("skip") skip: Int = 0
    ): DummyProductResponse

    @GET("products/categories")
    suspend fun getCategories(): List<DummyCategoryItem>
}

data class DummyCategoryItem(
    val slug: String,
    val name: String,
    val url: String
)
