package com.vicky.recsdk.model

data class RecoEvent(
    val eventType: EventType,
    val itemId: String,
    val timestamp: Long = System.currentTimeMillis()
)
