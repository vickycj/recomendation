package com.vicky.recsdk.ml

import com.vicky.recsdk.storage.entity.CoOccurrenceEntity
import com.vicky.recsdk.storage.entity.EventEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CoOccurrenceEngineTest {

    private lateinit var fakeDao: FakeCoOccurrenceDao
    private lateinit var engine: CoOccurrenceEngine
    private val fakeTime = object : com.vicky.recsdk.util.TimeProvider {
        override fun now(): Long = System.currentTimeMillis()
    }

    @Before
    fun setup() {
        fakeDao = FakeCoOccurrenceDao()
        engine = CoOccurrenceEngine(fakeDao, fakeTime)
    }

    @Test
    fun `empty events produce no co-occurrences`() = runBlocking {
        engine.rebuildMatrix(emptyList(), 86400000L)
        assertEquals(0, fakeDao.stored.size)
    }

    @Test
    fun `single event produces no co-occurrences`() = runBlocking {
        val events = listOf(
            EventEntity(1, "item1", "CLICK", 2.0, 1000L)
        )
        engine.rebuildMatrix(events, 86400000L)
        assertEquals(0, fakeDao.stored.size)
    }

    @Test
    fun `two events within window produce co-occurrence pair`() = runBlocking {
        val events = listOf(
            EventEntity(1, "item1", "CLICK", 2.0, 1000L),
            EventEntity(2, "item2", "CLICK", 2.0, 2000L)
        )
        engine.rebuildMatrix(events, 86400000L)
        // Should have 2 entries (bidirectional)
        assertTrue(fakeDao.stored.size >= 2)
        val item1Related = fakeDao.stored.filter { it.itemId1 == "item1" }
        assertTrue(item1Related.any { it.itemId2 == "item2" })
    }

    @Test
    fun `events outside window are not paired`() = runBlocking {
        val events = listOf(
            EventEntity(1, "item1", "CLICK", 2.0, 1000L),
            EventEntity(2, "item2", "CLICK", 2.0, 100_000_000L) // ~27 hours later
        )
        engine.rebuildMatrix(events, 86400000L) // 24h window
        assertEquals(0, fakeDao.stored.size)
    }

    @Test
    fun `same item events are not self-paired`() = runBlocking {
        val events = listOf(
            EventEntity(1, "item1", "CLICK", 2.0, 1000L),
            EventEntity(2, "item1", "VIEW", 1.0, 2000L)
        )
        engine.rebuildMatrix(events, 86400000L)
        assertEquals(0, fakeDao.stored.size)
    }

    @Test
    fun `higher event weights produce higher co-occurrence scores`() = runBlocking {
        val events = listOf(
            EventEntity(1, "item1", "PURCHASE", 8.0, 1000L),
            EventEntity(2, "item2", "PURCHASE", 8.0, 2000L),
            EventEntity(3, "item3", "VIEW", 1.0, 3000L),
            EventEntity(4, "item4", "VIEW", 1.0, 4000L)
        )
        engine.rebuildMatrix(events, 86400000L)

        // item1-item2 pair (8*8=64) should have higher score than item3-item4 (1*1=1)
        val pair12 = fakeDao.stored.find { it.itemId1 == "item1" && it.itemId2 == "item2" }
        val pair34 = fakeDao.stored.find { it.itemId1 == "item3" && it.itemId2 == "item4" }
        assertNotNull(pair12)
        assertNotNull(pair34)
        assertTrue(pair12!!.score > pair34!!.score)
    }

    @Test
    fun `getRelatedItems returns co-occurring items`() = runBlocking {
        // Pre-populate the DAO
        fakeDao.stored.addAll(listOf(
            CoOccurrenceEntity("item1", "item2", 0.8, 1000L),
            CoOccurrenceEntity("item1", "item3", 0.5, 1000L),
            CoOccurrenceEntity("item2", "item1", 0.8, 1000L)
        ))

        val related = engine.getRelatedItems(listOf("item1"))
        assertTrue(related.containsKey("item2"))
        assertTrue(related.containsKey("item3"))
        assertFalse(related.containsKey("item1")) // query item excluded
    }

    @Test
    fun `prepareRelatedItems and getCachedScore work together`() = runBlocking {
        fakeDao.stored.addAll(listOf(
            CoOccurrenceEntity("item1", "item2", 0.9, 1000L),
            CoOccurrenceEntity("item1", "item3", 0.4, 1000L)
        ))

        engine.prepareRelatedItems(listOf("item1"))

        assertTrue(engine.getCachedScore("item2") > 0.0)
        assertTrue(engine.getCachedScore("item3") > 0.0)
        assertEquals(0.0, engine.getCachedScore("item99"), 0.001) // unknown item
    }

    @Test
    fun `clearAll resets state`() = runBlocking {
        fakeDao.stored.add(CoOccurrenceEntity("item1", "item2", 0.9, 1000L))
        engine.prepareRelatedItems(listOf("item1"))
        assertTrue(engine.getCachedScore("item2") > 0.0)

        engine.clearAll()
        assertEquals(0.0, engine.getCachedScore("item2"), 0.001)
        assertTrue(fakeDao.stored.isEmpty())
    }
}

/**
 * Fake DAO for testing without Room.
 */
internal class FakeCoOccurrenceDao : com.vicky.recsdk.storage.dao.CoOccurrenceDao {
    val stored = mutableListOf<CoOccurrenceEntity>()

    override suspend fun insertAll(entities: List<CoOccurrenceEntity>) {
        stored.addAll(entities)
    }

    override suspend fun getCoOccurringItems(itemId: String): List<CoOccurrenceEntity> {
        return stored.filter { it.itemId1 == itemId }
    }

    override suspend fun getCoOccurringItemsForMultiple(itemIds: List<String>): List<CoOccurrenceEntity> {
        return stored.filter { it.itemId1 in itemIds }
    }

    override suspend fun deleteAll() {
        stored.clear()
    }
}
