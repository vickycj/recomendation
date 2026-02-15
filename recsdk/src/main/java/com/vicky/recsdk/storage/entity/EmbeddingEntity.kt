package com.vicky.recsdk.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reco_embeddings")
internal data class EmbeddingEntity(
    @PrimaryKey @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "vector_json") val vectorJson: String,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long
)
