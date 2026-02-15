package com.vicky.recsdk.ranking

import com.vicky.recsdk.ml.SimilarityEngine
import com.vicky.recsdk.model.RecoItem
import com.vicky.recsdk.model.UserProfile

/**
 * Scoring strategy based on text/semantic similarity.
 * Works with both TF-IDF (sparse) and TFLite (dense) embeddings
 * via the SimilarityEngine abstraction.
 */
internal class SemanticSimilarityScorer(
    private val engine: SimilarityEngine
) : ScoringStrategy {

    override fun prepare() {
        // Data is already prepared via engine.prepareUserEmbedding()
        // called by RecoEngine before ranking
    }

    override fun score(item: RecoItem, profile: UserProfile): Double {
        return engine.getCachedSimilarity(item.id)
    }

    override fun reason(item: RecoItem, profile: UserProfile): String? {
        val score = engine.getCachedSimilarity(item.id)
        return if (score > 0.1) "Similar to products you've shown interest in" else null
    }
}
