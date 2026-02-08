package com.vicky.recsdk.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reco_profile")
internal data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "profile_json") val profileJson: String,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long
)
