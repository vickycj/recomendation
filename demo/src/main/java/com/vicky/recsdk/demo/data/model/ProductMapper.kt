package com.vicky.recsdk.demo.data.model

import com.vicky.recsdk.model.RecoItem

object ProductMapper {

    fun toRecoItem(product: DummyProduct): RecoItem {
        return RecoItem(
            id = product.id.toString(),
            title = product.title,
            description = product.description,
            category = product.category,
            brand = product.brand ?: "",
            price = product.price,
            imageUrl = product.thumbnail,
            tags = product.tags,
            metadata = mapOf(
                "rating" to product.rating.toString(),
                "stock" to product.stock.toString()
            )
        )
    }

    fun toRecoItems(products: List<DummyProduct>): List<RecoItem> {
        return products.map { toRecoItem(it) }
    }
}
