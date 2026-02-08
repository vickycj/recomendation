package com.vicky.recsdk.model

data class RecoItem(
    val id: String,
    val title: String,
    val description: String = "",
    val category: String = "",
    val brand: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val tags: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)
