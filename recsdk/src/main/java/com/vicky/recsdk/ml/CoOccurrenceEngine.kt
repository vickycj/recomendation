package com.vicky.recsdk.ml

import com.vicky.recsdk.storage.dao.CoOccurrenceDao
import com.vicky.recsdk.storage.entity.CoOccurrenceEntity
import com.vicky.recsdk.storage.entity.EventEntity
import com.vicky.recsdk.util.TimeProvider

/**
 * Builds item-item co-occurrence matrix from user behavior events.
 * Items that appear together within a time window are considered co-occurring.
 * Stronger event types (purchase, add-to-cart) produce higher co-occurrence scores.
 */
internal class CoOccurrenceEngine(
    private val coOccurrenceDao: CoOccurrenceDao,
    private val timeProvider: TimeProvider
) {

    /**
     * Rebuild the co-occurrence matrix from all events.
     * Groups events into sessions by time window, then pairs items within each session.
     */
    suspend fun rebuildMatrix(events: List<EventEntity>, timeWindowMs: Long) {
        if (events.isEmpty()) return

        // Sort by timestamp
        val sorted = events.sortedBy { it.timestamp }

        // Build co-occurrence pairs using sliding time window
        val pairScores = mutableMapOf<Pair<String, String>, Double>()

        for (i in sorted.indices) {
            for (j in (i + 1) until sorted.size) {
                val timeDiff = sorted[j].timestamp - sorted[i].timestamp
                if (timeDiff > timeWindowMs) break // outside window

                val id1 = sorted[i].itemId
                val id2 = sorted[j].itemId
                if (id1 == id2) continue // skip self-pairing

                // Score = product of event weights, giving higher weight to purchase+purchase pairs
                val pairScore = sorted[i].eventWeight * sorted[j].eventWeight

                // Store both directions for easy lookup
                val key1 = if (id1 < id2) Pair(id1, id2) else Pair(id2, id1)
                pairScores[key1] = (pairScores[key1] ?: 0.0) + pairScore
            }
        }

        if (pairScores.isEmpty()) return

        // Normalize scores to [0, 1]
        val maxScore = pairScores.values.maxOrNull() ?: 1.0
        val now = timeProvider.now()

        val entities = pairScores.flatMap { (pair, score) ->
            val normalizedScore = score / maxScore
            listOf(
                CoOccurrenceEntity(pair.first, pair.second, normalizedScore, now),
                CoOccurrenceEntity(pair.second, pair.first, normalizedScore, now)
            )
        }

        coOccurrenceDao.deleteAll()
        coOccurrenceDao.insertAll(entities)
    }

    /**
     * Get items that co-occur with the given item IDs, with their scores.
     * Returns a map of itemId -> aggregated co-occurrence score.
     */
    suspend fun getRelatedItems(itemIds: List<String>): Map<String, Double> {
        if (itemIds.isEmpty()) return emptyMap()

        val entities = coOccurrenceDao.getCoOccurringItemsForMultiple(itemIds)
        val scores = mutableMapOf<String, Double>()

        for (entity in entities) {
            // Don't include query items in results
            if (entity.itemId2 in itemIds) continue
            scores[entity.itemId2] = (scores[entity.itemId2] ?: 0.0) + entity.score
        }

        // Normalize
        val maxScore = scores.values.maxOrNull() ?: return emptyMap()
        if (maxScore > 0.0) {
            for (key in scores.keys) {
                scores[key] = scores[key]!! / maxScore
            }
        }

        return scores
    }

    /**
     * Get related items synchronously (for use after prepare()).
     * Callers must call prepareRelatedItems() first.
     */
    @Volatile
    private var cachedRelatedItems: Map<String, Double> = emptyMap()

    suspend fun prepareRelatedItems(recentItemIds: List<String>) {
        cachedRelatedItems = getRelatedItems(recentItemIds)
    }

    fun getCachedScore(itemId: String): Double {
        return cachedRelatedItems[itemId] ?: 0.0
    }

    suspend fun clearAll() {
        coOccurrenceDao.deleteAll()
        cachedRelatedItems = emptyMap()
    }
}
