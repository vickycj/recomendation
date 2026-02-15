package com.vicky.recsdk.ranking

import com.vicky.recsdk.ml.CoOccurrenceEngine
import com.vicky.recsdk.model.RecoItem
import com.vicky.recsdk.model.UserProfile

/**
 * Scoring strategy based on item-item co-occurrence.
 * Items that frequently appear together with the user's recently interacted items
 * get higher scores. Uses pre-computed co-occurrence data via prepare().
 */
internal class CoOccurrenceScorer(
    private val coOccurrenceEngine: CoOccurrenceEngine
) : ScoringStrategy {

    override fun prepare() {
        // Data is already prepared via CoOccurrenceEngine.prepareRelatedItems()
        // called by RecoEngine before ranking
    }

    override fun score(item: RecoItem, profile: UserProfile): Double {
        return coOccurrenceEngine.getCachedScore(item.id)
    }

    override fun reason(item: RecoItem, profile: UserProfile): String? {
        val score = coOccurrenceEngine.getCachedScore(item.id)
        return if (score > 0.1) "Frequently browsed together with your interests" else null
    }
}
