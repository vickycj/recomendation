package com.vicky.recsdk.support

import com.vicky.recsdk.model.*
import com.vicky.recsdk.storage.entity.EventEntity
import com.vicky.recsdk.storage.entity.ItemCacheEntity
import com.vicky.recsdk.util.TimeProvider

internal class FakeTimeProvider(var fixedTime: Long = 1_700_000_000_000L) : TimeProvider {
    override fun now(): Long = fixedTime

    fun advanceDays(days: Int) {
        fixedTime += days.toLong() * 86_400_000L
    }

    fun advanceHours(hours: Int) {
        fixedTime += hours.toLong() * 3_600_000L
    }
}

internal object TestFixtures {

    val NOW = 1_700_000_000_000L

    fun clickEvent(itemId: String, daysAgo: Int = 0): EventEntity {
        return EventEntity(
            itemId = itemId,
            eventType = "CLICK",
            eventWeight = EventType.CLICK.weight,
            timestamp = NOW - (daysAgo.toLong() * 86_400_000L)
        )
    }

    fun viewEvent(itemId: String, daysAgo: Int = 0): EventEntity {
        return EventEntity(
            itemId = itemId,
            eventType = "VIEW",
            eventWeight = EventType.VIEW.weight,
            timestamp = NOW - (daysAgo.toLong() * 86_400_000L)
        )
    }

    fun purchaseEvent(itemId: String, daysAgo: Int = 0): EventEntity {
        return EventEntity(
            itemId = itemId,
            eventType = "PURCHASE",
            eventWeight = EventType.PURCHASE.weight,
            timestamp = NOW - (daysAgo.toLong() * 86_400_000L)
        )
    }

    fun addToCartEvent(itemId: String, daysAgo: Int = 0): EventEntity {
        return EventEntity(
            itemId = itemId,
            eventType = "ADD_TO_CART",
            eventWeight = EventType.ADD_TO_CART.weight,
            timestamp = NOW - (daysAgo.toLong() * 86_400_000L)
        )
    }

    fun favoriteEvent(itemId: String, daysAgo: Int = 0): EventEntity {
        return EventEntity(
            itemId = itemId,
            eventType = "FAVORITE",
            eventWeight = EventType.FAVORITE.weight,
            timestamp = NOW - (daysAgo.toLong() * 86_400_000L)
        )
    }

    fun electronicsItem(): RecoItem = RecoItem(
        id = "1",
        title = "Wireless Bluetooth Headphone",
        description = "Premium wireless bluetooth headphone with noise cancellation, USB-C charging",
        category = "electronics",
        brand = "Sony",
        price = 149.99,
        tags = listOf("headphone", "wireless", "bluetooth")
    )

    fun fitnessItem(): RecoItem = RecoItem(
        id = "2",
        title = "Protein Whey Powder",
        description = "Premium whey protein supplement for gym workout recovery, fitness enthusiasts",
        category = "fitness",
        brand = "Optimum",
        price = 49.99,
        tags = listOf("protein", "fitness", "supplement")
    )

    fun veganItem(): RecoItem = RecoItem(
        id = "3",
        title = "Organic Vegan Protein Bar",
        description = "Plant-based protein bar, dairy-free, cruelty-free vegan snack",
        category = "food",
        brand = "NatureWay",
        price = 3.99,
        tags = listOf("vegan", "organic", "protein")
    )

    fun meatItem(): RecoItem = RecoItem(
        id = "4",
        title = "Premium Mutton Curry Spice Mix",
        description = "Authentic lamb and mutton spice blend for delicious meat curry",
        category = "food",
        brand = "SpiceMaster",
        price = 8.99,
        tags = listOf("mutton", "spice", "meat")
    )

    fun fashionItem(): RecoItem = RecoItem(
        id = "5",
        title = "Designer Leather Jacket",
        description = "Stylish designer leather jacket for men, fashion blazer",
        category = "fashion",
        brand = "Nike",
        price = 299.99,
        tags = listOf("fashion", "jacket", "designer")
    )

    fun beautyItem(): RecoItem = RecoItem(
        id = "6",
        title = "Anti-Aging Skincare Serum",
        description = "Premium beauty skincare moisturizer serum with vitamin C",
        category = "beauty",
        brand = "Olay",
        price = 39.99,
        tags = listOf("beauty", "skincare", "serum")
    )

    fun genericItem(id: String, category: String = "misc", brand: String = "Generic"): RecoItem = RecoItem(
        id = id,
        title = "Generic Product $id",
        description = "A generic product with no special features",
        category = category,
        brand = brand,
        price = 9.99
    )

    fun itemCacheEntity(
        itemId: String,
        title: String = "Product $itemId",
        description: String = "",
        category: String = "",
        brand: String = "",
        tags: String = "[]"
    ): ItemCacheEntity = ItemCacheEntity(
        itemId = itemId,
        title = title,
        description = description,
        category = category,
        brand = brand,
        price = 9.99,
        imageUrl = "",
        tags = tags,
        metadata = "{}",
        lastUpdated = NOW
    )
}
