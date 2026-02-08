package com.vicky.recsdk.model

data class RecoResult(
    val items: List<ScoredItem> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)

data class ScoredItem(
    val item: RecoItem,
    val score: Double,
    val reasons: List<String> = emptyList()
)
