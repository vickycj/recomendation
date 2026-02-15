package com.vicky.recsdk.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vicky.recsdk.storage.entity.CoOccurrenceEntity

@Dao
internal interface CoOccurrenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CoOccurrenceEntity>)

    @Query("SELECT * FROM reco_co_occurrence WHERE item_id_1 = :itemId ORDER BY score DESC")
    suspend fun getCoOccurringItems(itemId: String): List<CoOccurrenceEntity>

    @Query("SELECT * FROM reco_co_occurrence WHERE item_id_1 IN (:itemIds) ORDER BY score DESC")
    suspend fun getCoOccurringItemsForMultiple(itemIds: List<String>): List<CoOccurrenceEntity>

    @Query("DELETE FROM reco_co_occurrence")
    suspend fun deleteAll()
}
