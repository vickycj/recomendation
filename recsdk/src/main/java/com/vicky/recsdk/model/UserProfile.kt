package com.vicky.recsdk.model

data class UserProfile(
    val topCategories: List<CategoryAffinity> = emptyList(),
    val topBrands: List<BrandAffinity> = emptyList(),
    val interestTags: List<InterestTagScore> = emptyList(),
    val lastUpdated: Long = 0
)

data class CategoryAffinity(val category: String, val score: Double)

data class BrandAffinity(val brand: String, val score: Double)

data class InterestTagScore(val tag: String, val score: Double)
