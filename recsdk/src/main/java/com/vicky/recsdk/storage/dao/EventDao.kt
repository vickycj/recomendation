package com.vicky.recsdk.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.vicky.recsdk.storage.entity.EventEntity

@Dao
internal interface EventDao {

    @Insert
    suspend fun insertEvent(event: EventEntity)

    @Query("SELECT * FROM reco_events WHERE item_id = :itemId ORDER BY timestamp DESC")
    suspend fun getEventsForItem(itemId: String): List<EventEntity>

    @Query("SELECT * FROM reco_events ORDER BY timestamp DESC")
    suspend fun getAllEvents(): List<EventEntity>

    @Query("SELECT * FROM reco_events WHERE timestamp > :since ORDER BY timestamp DESC")
    suspend fun getEventsSince(since: Long): List<EventEntity>

    @Query("DELETE FROM reco_events WHERE timestamp < :before")
    suspend fun deleteEventsBefore(before: Long)

    @Query("DELETE FROM reco_events")
    suspend fun deleteAll()
}
