package com.vicky.recsdk.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "reco_co_occurrence",
    primaryKeys = ["item_id_1", "item_id_2"]
)
internal data class CoOccurrenceEntity(
    @ColumnInfo(name = "item_id_1") val itemId1: String,
    @ColumnInfo(name = "item_id_2") val itemId2: String,
    @ColumnInfo(name = "score") val score: Double,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long
)
