package com.vicky.recsdk.ranking

import com.vicky.recsdk.model.RecoItem
import com.vicky.recsdk.model.UserProfile

internal class CategoryAffinityScorer : ScoringStrategy {

    override fun score(item: RecoItem, profile: UserProfile): Double {
        if (item.category.isBlank()) return 0.0
        return profile.topCategories
            .find { it.category.equals(item.category, ignoreCase = true) }
            ?.score ?: 0.0
    }

    override fun reason(item: RecoItem, profile: UserProfile): String? {
        if (item.category.isBlank()) return null
        val match = profile.topCategories
            .find { it.category.equals(item.category, ignoreCase = true) }
        return match?.let { "In your top category: ${it.category}" }
    }
}
