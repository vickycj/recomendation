package com.vicky.recsdk

data class RecoConfig(
    val maxRecommendations: Int = 20,
    val recencyHalfLifeDays: Int = 7,
    val profileRebuildIntervalMs: Long = 300_000L,
    val enableTextClassification: Boolean = true,
    val eventRetentionDays: Int = 90,
    val enableDebugLogging: Boolean = false
)
