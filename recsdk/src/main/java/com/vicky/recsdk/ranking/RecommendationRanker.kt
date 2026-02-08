package com.vicky.recsdk.ranking

import com.vicky.recsdk.model.RecoItem
import com.vicky.recsdk.model.RecoResult
import com.vicky.recsdk.model.ScoredItem
import com.vicky.recsdk.model.UserProfile

internal class RecommendationRanker(
    private val strategies: List<ScoringStrategy>,
    private val weights: List<Double> = listOf(0.35, 0.30, 0.35)
) {

    init {
        require(strategies.size == weights.size) {
            "strategies and weights must have the same size"
        }
    }

    fun rank(items: List<RecoItem>, profile: UserProfile, limit: Int): RecoResult {
        if (profile.topCategories.isEmpty() && profile.topBrands.isEmpty() && profile.interestTags.isEmpty()) {
            return RecoResult(emptyList(), System.currentTimeMillis())
        }

        val scoredItems = items.map { item ->
            val totalScore = strategies.zip(weights).sumOf { (strategy, weight) ->
                strategy.score(item, profile) * weight
            }
            val reasons = strategies.mapNotNull { it.reason(item, profile) }
            ScoredItem(item, totalScore, reasons)
        }
            .filter { it.score > 0.0 }
            .sortedByDescending { it.score }
            .take(limit)

        return RecoResult(scoredItems, System.currentTimeMillis())
    }
}
