package com.vicky.recsdk.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reco_events")
internal data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "event_type") val eventType: String,
    @ColumnInfo(name = "event_weight") val eventWeight: Double,
    @ColumnInfo(name = "timestamp") val timestamp: Long
)
