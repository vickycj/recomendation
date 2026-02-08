package com.vicky.recsdk.util

import kotlin.math.exp

internal object RecencyWeightUtil {

    fun computeWeight(eventTimestamp: Long, currentTime: Long, halfLifeDays: Int): Double {
        val daysSince = (currentTime - eventTimestamp) / 86_400_000.0
        if (daysSince < 0) return 1.0
        return exp(-0.693 * daysSince / halfLifeDays)
    }
}
