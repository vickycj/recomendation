package com.vicky.recsdk.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reco_items")
internal data class ItemCacheEntity(
    @PrimaryKey @ColumnInfo(name = "item_id") val itemId: String,
    val title: String,
    val description: String,
    val category: String,
    val brand: String,
    val price: Double,
    @ColumnInfo(name = "image_url") val imageUrl: String,
    val tags: String,
    val metadata: String,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long
)
