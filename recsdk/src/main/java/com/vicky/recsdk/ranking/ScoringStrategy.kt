package com.vicky.recsdk.ranking

import com.vicky.recsdk.model.RecoItem
import com.vicky.recsdk.model.UserProfile

internal interface ScoringStrategy {
    /**
     * Pre-load any data needed for synchronous score() calls.
     * Default no-op — ML scorers override to pre-compute data.
     */
    fun prepare() {}

    fun score(item: RecoItem, profile: UserProfile): Double
    fun reason(item: RecoItem, profile: UserProfile): String?
}
