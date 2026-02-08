package com.vicky.recsdk.tracking

import com.vicky.recsdk.model.RecoEvent
import com.vicky.recsdk.storage.dao.EventDao
import com.vicky.recsdk.storage.entity.EventEntity
import com.vicky.recsdk.util.TimeProvider

internal class EventTracker(
    private val eventDao: EventDao,
    private val timeProvider: TimeProvider
) {

    suspend fun trackEvent(event: RecoEvent) {
        val entity = EventEntity(
            itemId = event.itemId,
            eventType = event.eventType.name,
            eventWeight = event.eventType.weight,
            timestamp = event.timestamp
        )
        eventDao.insertEvent(entity)
    }

    suspend fun getEventsForItem(itemId: String): List<EventEntity> {
        return eventDao.getEventsForItem(itemId)
    }

    suspend fun getAllEvents(): List<EventEntity> {
        return eventDao.getAllEvents()
    }

    suspend fun pruneOldEvents(retentionDays: Int) {
        val cutoff = timeProvider.now() - (retentionDays.toLong() * 86_400_000L)
        eventDao.deleteEventsBefore(cutoff)
    }

    suspend fun clearAll() {
        eventDao.deleteAll()
    }
}
