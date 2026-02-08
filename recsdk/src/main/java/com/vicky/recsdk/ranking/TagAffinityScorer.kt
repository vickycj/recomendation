package com.vicky.recsdk.ranking

import com.vicky.recsdk.model.RecoItem
import com.vicky.recsdk.model.UserProfile
import com.vicky.recsdk.profile.ItemClassifier

internal class TagAffinityScorer(
    private val classifier: ItemClassifier
) : ScoringStrategy {

    override fun score(item: RecoItem, profile: UserProfile): Double {
        if (profile.interestTags.isEmpty()) return 0.0

        val itemTags = classifier.classify(item.title, item.description, item.tags)
        if (itemTags.isEmpty()) return 0.0

        val profileTagMap = profile.interestTags.associate { it.tag to it.score }

        // Dot product of item tag vector with user profile tag vector
        var dotProduct = 0.0
        for ((tag, confidence) in itemTags) {
            val profileScore = profileTagMap[tag] ?: 0.0
            dotProduct += confidence * profileScore
        }

        // Normalize by max possible score
        val maxPossible = itemTags.values.sum()
        return if (maxPossible > 0) dotProduct / maxPossible else 0.0
    }

    override fun reason(item: RecoItem, profile: UserProfile): String? {
        if (profile.interestTags.isEmpty()) return null

        val itemTags = classifier.classify(item.title, item.description, item.tags)
        val profileTagMap = profile.interestTags.associate { it.tag to it.score }

        // Find the strongest matching tag
        val bestMatch = itemTags
            .filter { (tag, _) -> (profileTagMap[tag] ?: 0.0) > 0.0 }
            .maxByOrNull { (tag, confidence) -> confidence * (profileTagMap[tag] ?: 0.0) }

        return bestMatch?.let { "Matches your interest: ${it.key}" }
    }
}
