package com.vicky.recsdk.tracking

import com.vicky.recsdk.support.FakeTimeProvider
import com.vicky.recsdk.support.TestFixtures
import com.vicky.recsdk.support.TestFixtures.NOW
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BehaviorScorerTest {

    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var scorer: BehaviorScorer

    @Before
    fun setUp() {
        timeProvider = FakeTimeProvider(NOW)
        scorer = BehaviorScorer(recencyHalfLifeDays = 7, timeProvider = timeProvider)
    }

    @Test
    fun `event from today has full weight`() {
        val events = listOf(TestFixtures.clickEvent("1", daysAgo = 0))
        val score = scorer.computeItemScore(events)
        // CLICK weight = 2.0, recency = 1.0 (today)
        assertEquals(2.0, score, 0.01)
    }

    @Test
    fun `event from 7 days ago has half weight`() {
        val events = listOf(TestFixtures.clickEvent("1", daysAgo = 7))
        val score = scorer.computeItemScore(events)
        // CLICK weight = 2.0, recency = 0.5 (half-life)
        assertEquals(1.0, score, 0.05)
    }

    @Test
    fun `event from 14 days ago has quarter weight`() {
        val events = listOf(TestFixtures.clickEvent("1", daysAgo = 14))
        val score = scorer.computeItemScore(events)
        // CLICK weight = 2.0, recency = 0.25 (two half-lives)
        assertEquals(0.5, score, 0.05)
    }

    @Test
    fun `purchase event weighs more than click`() {
        val click = listOf(TestFixtures.clickEvent("1", daysAgo = 0))
        val purchase = listOf(TestFixtures.purchaseEvent("1", daysAgo = 0))

        val clickScore = scorer.computeItemScore(click)
        val purchaseScore = scorer.computeItemScore(purchase)

        assertTrue("Purchase ($purchaseScore) should be > Click ($clickScore)",
            purchaseScore > clickScore)
        assertEquals(8.0, purchaseScore, 0.01)
    }

    @Test
    fun `multiple events accumulate`() {
        val events = listOf(
            TestFixtures.clickEvent("1", daysAgo = 0),
            TestFixtures.clickEvent("1", daysAgo = 0),
            TestFixtures.viewEvent("1", daysAgo = 0)
        )
        val score = scorer.computeItemScore(events)
        // 2.0 + 2.0 + 1.0 = 5.0
        assertEquals(5.0, score, 0.01)
    }

    @Test
    fun `mixed events with different recency`() {
        val events = listOf(
            TestFixtures.clickEvent("1", daysAgo = 0),   // 2.0 * 1.0 = 2.0
            TestFixtures.clickEvent("1", daysAgo = 7)    // 2.0 * 0.5 = 1.0
        )
        val score = scorer.computeItemScore(events)
        assertEquals(3.0, score, 0.1)
    }

    @Test
    fun `category scores are normalized`() {
        val events = listOf(
            TestFixtures.clickEvent("1", daysAgo = 0),
            TestFixtures.clickEvent("1", daysAgo = 0),
            TestFixtures.clickEvent("2", daysAgo = 0)
        )
        val itemCategoryMap = mapOf("1" to "electronics", "2" to "fashion")

        val scores = scorer.computeCategoryScores(events, itemCategoryMap)

        // electronics has 2 clicks = 4.0, fashion has 1 click = 2.0
        // Normalized: electronics = 1.0, fashion = 0.5
        assertEquals(1.0, scores["electronics"]!!, 0.01)
        assertEquals(0.5, scores["fashion"]!!, 0.01)
    }

    @Test
    fun `brand scores are normalized`() {
        val events = listOf(
            TestFixtures.clickEvent("1", daysAgo = 0),
            TestFixtures.purchaseEvent("2", daysAgo = 0)
        )
        val itemBrandMap = mapOf("1" to "Sony", "2" to "Nike")

        val scores = scorer.computeBrandScores(events, itemBrandMap)

        // Sony: 2.0 (click), Nike: 8.0 (purchase)
        // Normalized: Nike = 1.0, Sony = 0.25
        assertEquals(1.0, scores["Nike"]!!, 0.01)
        assertEquals(0.25, scores["Sony"]!!, 0.01)
    }

    @Test
    fun `empty events return zero score`() {
        val score = scorer.computeItemScore(emptyList())
        assertEquals(0.0, score, 0.01)
    }

    @Test
    fun `blank category is ignored`() {
        val events = listOf(TestFixtures.clickEvent("1", daysAgo = 0))
        val itemCategoryMap = mapOf("1" to "")

        val scores = scorer.computeCategoryScores(events, itemCategoryMap)
        assertTrue(scores.isEmpty())
    }

    @Test
    fun `recency weight for future events is capped at 1`() {
        val weight = scorer.recencyWeight(NOW + 100_000L)
        assertEquals(1.0, weight, 0.01)
    }
}
