package com.vicky.recsdk.ml

import com.vicky.recsdk.model.RecoItem
import com.vicky.recsdk.storage.entity.EventEntity

/**
 * Internal interface shared by TfIdfEngine and EmbeddingBridge.
 * Allows SemanticSimilarityScorer to work with either engine type.
 */
internal interface SimilarityEngine {

    /** Build embeddings for all items and persist to storage. */
    suspend fun buildAndStore(items: List<RecoItem>)

    /** Prepare user embedding based on interaction history. */
    fun prepareUserEmbedding(events: List<EventEntity>)

    /** Get cosine similarity between cached user embedding and an item. */
    fun getCachedSimilarity(itemId: String): Double

    /** Load persisted embeddings from storage. */
    suspend fun loadPersistedEmbeddings()

    /** Clear all stored embeddings and reset state. */
    suspend fun clearAll()
}
