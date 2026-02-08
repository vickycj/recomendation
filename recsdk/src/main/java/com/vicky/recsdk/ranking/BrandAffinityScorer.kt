package com.vicky.recsdk.ranking

import com.vicky.recsdk.model.RecoItem
import com.vicky.recsdk.model.UserProfile

internal class BrandAffinityScorer : ScoringStrategy {

    override fun score(item: RecoItem, profile: UserProfile): Double {
        if (item.brand.isBlank()) return 0.0
        return profile.topBrands
            .find { it.brand.equals(item.brand, ignoreCase = true) }
            ?.score ?: 0.0
    }

    override fun reason(item: RecoItem, profile: UserProfile): String? {
        if (item.brand.isBlank()) return null
        val match = profile.topBrands
            .find { it.brand.equals(item.brand, ignoreCase = true) }
        return match?.let { "Matches your favorite brand: ${it.brand}" }
    }
}
