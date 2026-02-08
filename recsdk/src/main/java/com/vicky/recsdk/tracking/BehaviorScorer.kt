package com.vicky.recsdk.tracking

import com.vicky.recsdk.storage.entity.EventEntity
import com.vicky.recsdk.util.TimeProvider
import kotlin.math.exp

internal class BehaviorScorer(
    private val recencyHalfLifeDays: Int,
    private val timeProvider: TimeProvider
) {

    fun computeItemScore(events: List<EventEntity>): Double {
        return events.sumOf { event ->
            event.eventWeight * recencyWeight(event.timestamp)
        }
    }

    fun computeCategoryScores(
        events: List<EventEntity>,
        itemCategoryMap: Map<String, String>
    ): Map<String, Double> {
        val scores = mutableMapOf<String, Double>()
        for (event in events) {
            val category = itemCategoryMap[event.itemId] ?: continue
            if (category.isBlank()) continue
            val eventScore = event.eventWeight * recencyWeight(event.timestamp)
            scores[category] = (scores[category] ?: 0.0) + eventScore
        }
        return normalizeScores(scores)
    }

    fun computeBrandScores(
        events: List<EventEntity>,
        itemBrandMap: Map<String, String>
    ): Map<String, Double> {
        val scores = mutableMapOf<String, Double>()
        for (event in events) {
            val brand = itemBrandMap[event.itemId] ?: continue
            if (brand.isBlank()) continue
            val eventScore = event.eventWeight * recencyWeight(event.timestamp)
            scores[brand] = (scores[brand] ?: 0.0) + eventScore
        }
        return normalizeScores(scores)
    }

    internal fun recencyWeight(eventTimestamp: Long): Double {
        val daysSince = (timeProvider.now() - eventTimestamp) / 86_400_000.0
        if (daysSince < 0) return 1.0
        return exp(-0.693 * daysSince / recencyHalfLifeDays)
    }

    private fun normalizeScores(scores: Map<String, Double>): Map<String, Double> {
        val maxScore = scores.values.maxOrNull() ?: return scores
        if (maxScore == 0.0) return scores
        return scores.mapValues { it.value / maxScore }
    }
}
