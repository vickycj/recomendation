package com.vicky.recsdk.ranking

import com.vicky.recsdk.model.RecoItem
import com.vicky.recsdk.model.UserProfile

internal interface ScoringStrategy {
    fun score(item: RecoItem, profile: UserProfile): Double
    fun reason(item: RecoItem, profile: UserProfile): String?
}
