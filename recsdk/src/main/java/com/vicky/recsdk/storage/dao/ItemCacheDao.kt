package com.vicky.recsdk.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vicky.recsdk.storage.entity.ItemCacheEntity

@Dao
internal interface ItemCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItemCacheEntity>)

    @Query("SELECT * FROM reco_items WHERE item_id = :itemId")
    suspend fun getItem(itemId: String): ItemCacheEntity?

    @Query("SELECT * FROM reco_items")
    suspend fun getAllItems(): List<ItemCacheEntity>

    @Query("SELECT * FROM reco_items WHERE item_id IN (:itemIds)")
    suspend fun getItemsByIds(itemIds: List<String>): List<ItemCacheEntity>

    @Query("DELETE FROM reco_items")
    suspend fun deleteAll()
}
