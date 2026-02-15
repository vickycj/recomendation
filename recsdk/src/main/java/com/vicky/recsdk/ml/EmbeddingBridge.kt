package com.vicky.recsdk.ml

import com.vicky.recsdk.model.RecoItem
import com.vicky.recsdk.storage.dao.EmbeddingDao
import com.vicky.recsdk.storage.entity.EmbeddingEntity
import com.vicky.recsdk.storage.entity.EventEntity
import com.vicky.recsdk.util.TimeProvider

/**
 * Bridges an external [EmbeddingProvider] with the SDK's internal storage and scoring.
 * Handles persistence, user embedding computation, and similarity caching.
 * The provider only computes embeddings; this class handles everything else.
 */
internal class EmbeddingBridge(
    private val provider: EmbeddingProvider,
    private val embeddingDao: EmbeddingDao,
    private val timeProvider: TimeProvider
) : SimilarityEngine {

    private var itemEmbeddings: Map<String, DenseVector> = emptyMap()

    @Volatile
    private var cachedUserEmbedding: DenseVector? = null

    override suspend fun buildAndStore(items: List<RecoItem>) {
        if (items.isEmpty()) return

        val now = timeProvider.now()
        val embeddings = mutableMapOf<String, DenseVector>()
        val entities = mutableListOf<EmbeddingEntity>()

        for (item in items) {
            val text = buildItemText(item)
            val floats = provider.embedText(text)
            val vector = DenseVector(floats)
            embeddings[item.id] = vector
            entities.add(EmbeddingEntity(item.id, vector.toJson(), now))
        }

        itemEmbeddings = embeddings

        embeddingDao.deleteAll()
        embeddingDao.insertAll(entities)
    }

    override fun prepareUserEmbedding(events: List<EventEntity>) {
        if (events.isEmpty() || itemEmbeddings.isEmpty()) {
            cachedUserEmbedding = DenseVector.zero(actualDimensions())
            return
        }

        var combined: DenseVector? = null
        var totalWeight = 0f

        for (event in events) {
            val itemVector = itemEmbeddings[event.itemId] ?: continue
            val scaled = itemVector.scale(event.eventWeight.toFloat())
            combined = if (combined == null) {
                scaled
            } else {
                combined.add(scaled)
            }
            totalWeight += event.eventWeight.toFloat()
        }

        cachedUserEmbedding = if (combined != null && totalWeight > 0f) {
            combined.scale(1f / totalWeight)
        } else {
            DenseVector.zero(actualDimensions())
        }
    }

    /**
     * Get actual dimensions from stored embeddings (more reliable than provider.dimensions()
     * since the model may report different dimensions than what it actually produces).
     */
    private fun actualDimensions(): Int {
        return itemEmbeddings.values.firstOrNull()?.size ?: provider.dimensions()
    }

    override fun getCachedSimilarity(itemId: String): Double {
        val userEmb = cachedUserEmbedding ?: return 0.0
        val itemVector = itemEmbeddings[itemId] ?: return 0.0
        return userEmb.cosineSimilarity(itemVector)
    }

    override suspend fun loadPersistedEmbeddings() {
        val entities = embeddingDao.getAllEmbeddings()
        if (entities.isEmpty()) return

        val loaded = mutableMapOf<String, DenseVector>()
        for (entity in entities) {
            try {
                loaded[entity.itemId] = DenseVector.fromJson(entity.vectorJson)
            } catch (_: Exception) {
                // Skip corrupt entries
            }
        }
        itemEmbeddings = loaded
    }

    override suspend fun clearAll() {
        embeddingDao.deleteAll()
        itemEmbeddings = emptyMap()
        cachedUserEmbedding = null
    }

    private fun buildItemText(item: RecoItem): String {
        return buildString {
            append(item.title)
            append(" ")
            append(item.description)
            append(" ")
            append(item.category)
            append(" ")
            append(item.brand)
            append(" ")
            item.tags.forEach { append(it); append(" ") }
        }.trim()
    }
}
