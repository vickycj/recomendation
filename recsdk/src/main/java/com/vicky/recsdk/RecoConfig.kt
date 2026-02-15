package com.vicky.recsdk

import com.vicky.recsdk.ml.EmbeddingProvider

data class RecoConfig(
    val maxRecommendations: Int = 20,
    val recencyHalfLifeDays: Int = 7,
    val profileRebuildIntervalMs: Long = 300_000L,
    val enableTextClassification: Boolean = true,
    val eventRetentionDays: Int = 90,
    val enableDebugLogging: Boolean = false,
    /** Enable co-occurrence based recommendations (item-item collaborative filtering) */
    val enableCoOccurrence: Boolean = true,
    /** Enable semantic similarity based recommendations (TF-IDF by default, TFLite if provider set) */
    val enableSemanticSimilarity: Boolean = true,
    /**
     * Optional custom embedding provider (e.g., TFLite-based).
     * If null, uses the built-in TF-IDF engine (zero external dependencies).
     * If set, replaces TF-IDF with semantic embeddings from the provider.
     */
    val embeddingProvider: EmbeddingProvider? = null,
    /** Time window in hours for co-occurrence session grouping */
    val coOccurrenceTimeWindowHours: Int = 24
)
