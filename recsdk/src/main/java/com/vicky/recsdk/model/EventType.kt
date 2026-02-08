package com.vicky.recsdk.model

enum class EventType(val weight: Double) {
    VIEW(1.0),
    CLICK(2.0),
    ADD_TO_CART(4.0),
    PURCHASE(8.0),
    SEARCH(1.5),
    FAVORITE(5.0)
}
