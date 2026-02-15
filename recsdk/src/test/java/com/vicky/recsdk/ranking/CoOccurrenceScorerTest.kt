package com.vicky.recsdk.ranking

import com.vicky.recsdk.ml.CoOccurrenceEngine
import com.vicky.recsdk.ml.FakeCoOccurrenceDao
import com.vicky.recsdk.model.RecoItem
import com.vicky.recsdk.model.UserProfile
import com.vicky.recsdk.storage.entity.CoOccurrenceEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CoOccurrenceScorerTest {

    private lateinit var fakeDao: FakeCoOccurrenceDao
    private lateinit var engine: CoOccurrenceEngine
    private lateinit var scorer: CoOccurrenceScorer
    private val emptyProfile = UserProfile(emptyList(), emptyList(), emptyList(), 0L)
    private val fakeTime = object : com.vicky.recsdk.util.TimeProvider {
        override fun now(): Long = System.currentTimeMillis()
    }

    private fun createItem(id: String) = RecoItem(
        id = id, title = "Test $id", description = "Desc", category = "Cat",
        brand = "Brand", price = 10.0, imageUrl = ""
    )

    @Before
    fun setup() {
        fakeDao = FakeCoOccurrenceDao()
        engine = CoOccurrenceEngine(fakeDao, fakeTime)
        scorer = CoOccurrenceScorer(engine)
    }

    @Test
    fun `score returns 0 for item with no co-occurrence`() = runBlocking {
        engine.prepareRelatedItems(listOf("item1"))
        assertEquals(0.0, scorer.score(createItem("item99"), emptyProfile), 0.001)
    }

    @Test
    fun `score returns positive for co-occurring item`() = runBlocking {
        fakeDao.stored.addAll(listOf(
            CoOccurrenceEntity("item1", "item2", 0.8, 1000L)
        ))
        engine.prepareRelatedItems(listOf("item1"))

        val score = scorer.score(createItem("item2"), emptyProfile)
        assertTrue("Score should be positive for co-occurring item", score > 0.0)
    }

    @Test
    fun `higher co-occurrence score produces higher scorer output`() = runBlocking {
        fakeDao.stored.addAll(listOf(
            CoOccurrenceEntity("item1", "item2", 0.9, 1000L),
            CoOccurrenceEntity("item1", "item3", 0.3, 1000L)
        ))
        engine.prepareRelatedItems(listOf("item1"))

        val scoreHigh = scorer.score(createItem("item2"), emptyProfile)
        val scoreLow = scorer.score(createItem("item3"), emptyProfile)
        assertTrue("item2 ($scoreHigh) should score higher than item3 ($scoreLow)",
            scoreHigh > scoreLow)
    }

    @Test
    fun `reason returns message for significant co-occurrence`() = runBlocking {
        fakeDao.stored.add(CoOccurrenceEntity("item1", "item2", 0.9, 1000L))
        engine.prepareRelatedItems(listOf("item1"))

        val reason = scorer.reason(createItem("item2"), emptyProfile)
        assertNotNull(reason)
        assertTrue(reason!!.contains("browsed together"))
    }

    @Test
    fun `reason returns null for weak co-occurrence`() = runBlocking {
        engine.prepareRelatedItems(listOf("item1"))
        val reason = scorer.reason(createItem("item99"), emptyProfile)
        assertNull(reason)
    }
}
