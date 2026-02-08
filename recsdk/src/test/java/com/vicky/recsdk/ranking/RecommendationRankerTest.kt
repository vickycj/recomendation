package com.vicky.recsdk.ranking

import com.vicky.recsdk.model.*
import com.vicky.recsdk.profile.KeywordClassifier
import com.vicky.recsdk.support.TestFixtures
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RecommendationRankerTest {

    private lateinit var ranker: RecommendationRanker

    @Before
    fun setUp() {
        val classifier = KeywordClassifier()
        ranker = RecommendationRanker(
            strategies = listOf(
                CategoryAffinityScorer(),
                BrandAffinityScorer(),
                TagAffinityScorer(classifier)
            )
        )
    }

    @Test
    fun `items matching user top category ranked higher`() {
        val profile = UserProfile(
            topCategories = listOf(CategoryAffinity("electronics", 1.0)),
            topBrands = emptyList(),
            interestTags = emptyList(),
            lastUpdated = TestFixtures.NOW
        )
        val items = listOf(
            TestFixtures.electronicsItem(),
            TestFixtures.fashionItem()
        )

        val result = ranker.rank(items, profile, 10)

        assertTrue("Should have results", result.items.isNotEmpty())
        assertEquals("Electronics item should be first", "1", result.items.first().item.id)
    }

    @Test
    fun `items matching user top brand ranked higher`() {
        val profile = UserProfile(
            topCategories = emptyList(),
            topBrands = listOf(BrandAffinity("Nike", 1.0)),
            interestTags = emptyList(),
            lastUpdated = TestFixtures.NOW
        )
        val items = listOf(
            TestFixtures.electronicsItem(),  // Sony
            TestFixtures.fashionItem()       // Nike
        )

        val result = ranker.rank(items, profile, 10)

        assertTrue("Should have results", result.items.isNotEmpty())
        assertEquals("Nike item should be first", "5", result.items.first().item.id)
    }

    @Test
    fun `items matching interest tags ranked higher`() {
        val profile = UserProfile(
            topCategories = emptyList(),
            topBrands = emptyList(),
            interestTags = listOf(InterestTagScore("Fitness", 1.0)),
            lastUpdated = TestFixtures.NOW
        )
        val items = listOf(
            TestFixtures.electronicsItem(),
            TestFixtures.fitnessItem()
        )

        val result = ranker.rank(items, profile, 10)

        assertTrue("Should have results", result.items.isNotEmpty())
        assertEquals("Fitness item should be first", "2", result.items.first().item.id)
    }

    @Test
    fun `items with no affinity are filtered out`() {
        val profile = UserProfile(
            topCategories = listOf(CategoryAffinity("electronics", 1.0)),
            topBrands = emptyList(),
            interestTags = emptyList(),
            lastUpdated = TestFixtures.NOW
        )
        val items = listOf(
            TestFixtures.genericItem("99", category = "unknown_category", brand = "UnknownBrand")
        )

        val result = ranker.rank(items, profile, 10)

        assertTrue("Items with zero score should be filtered", result.items.isEmpty())
    }

    @Test
    fun `limit parameter is respected`() {
        val profile = UserProfile(
            topCategories = listOf(
                CategoryAffinity("electronics", 1.0),
                CategoryAffinity("food", 0.8),
                CategoryAffinity("fashion", 0.6),
                CategoryAffinity("beauty", 0.4)
            ),
            lastUpdated = TestFixtures.NOW
        )
        val items = listOf(
            TestFixtures.electronicsItem(),
            TestFixtures.veganItem(),
            TestFixtures.meatItem(),
            TestFixtures.fashionItem(),
            TestFixtures.beautyItem()
        )

        val result = ranker.rank(items, profile, 2)

        assertEquals("Should return only 2 items", 2, result.items.size)
    }

    @Test
    fun `empty profile returns empty results`() {
        val profile = UserProfile(lastUpdated = TestFixtures.NOW)
        val items = listOf(TestFixtures.electronicsItem(), TestFixtures.fitnessItem())

        val result = ranker.rank(items, profile, 10)

        assertTrue("Empty profile should return empty results", result.items.isEmpty())
    }

    @Test
    fun `scored items include reasons`() {
        val profile = UserProfile(
            topCategories = listOf(CategoryAffinity("electronics", 1.0)),
            topBrands = listOf(BrandAffinity("Sony", 1.0)),
            interestTags = listOf(InterestTagScore("Tech & Electronics", 1.0)),
            lastUpdated = TestFixtures.NOW
        )
        val items = listOf(TestFixtures.electronicsItem())

        val result = ranker.rank(items, profile, 10)

        assertTrue("Should have results", result.items.isNotEmpty())
        val reasons = result.items.first().reasons
        assertTrue("Should have reasons, got: $reasons", reasons.isNotEmpty())
    }

    @Test
    fun `vegan user gets vegan products recommended`() {
        val profile = UserProfile(
            topCategories = listOf(CategoryAffinity("food", 1.0)),
            interestTags = listOf(
                InterestTagScore("Vegan", 1.0),
                InterestTagScore("Organic", 0.8)
            ),
            lastUpdated = TestFixtures.NOW
        )
        val items = listOf(
            TestFixtures.veganItem(),
            TestFixtures.meatItem(),
            TestFixtures.electronicsItem()
        )

        val result = ranker.rank(items, profile, 10)

        assertTrue("Should have results", result.items.isNotEmpty())
        // Vegan item should score higher than meat item for a vegan user
        val veganScore = result.items.find { it.item.id == "3" }?.score ?: 0.0
        val meatScore = result.items.find { it.item.id == "4" }?.score ?: 0.0
        assertTrue("Vegan ($veganScore) should score >= Meat ($meatScore) for vegan user",
            veganScore >= meatScore)
    }

    @Test
    fun `mutton lover gets meat products recommended`() {
        val profile = UserProfile(
            topCategories = listOf(CategoryAffinity("food", 1.0)),
            interestTags = listOf(
                InterestTagScore("Non-Vegetarian", 1.0)
            ),
            lastUpdated = TestFixtures.NOW
        )
        val items = listOf(
            TestFixtures.veganItem(),
            TestFixtures.meatItem()
        )

        val result = ranker.rank(items, profile, 10)

        assertTrue("Should have results", result.items.isNotEmpty())
        val meatScore = result.items.find { it.item.id == "4" }?.score ?: 0.0
        val veganScore = result.items.find { it.item.id == "3" }?.score ?: 0.0
        assertTrue("Meat ($meatScore) should score > Vegan ($veganScore) for meat lover",
            meatScore > veganScore)
    }

    @Test
    fun `combined scoring uses all three strategies`() {
        val profile = UserProfile(
            topCategories = listOf(CategoryAffinity("electronics", 1.0)),
            topBrands = listOf(BrandAffinity("Sony", 1.0)),
            interestTags = listOf(InterestTagScore("Tech & Electronics", 1.0)),
            lastUpdated = TestFixtures.NOW
        )

        val items = listOf(TestFixtures.electronicsItem())
        val result = ranker.rank(items, profile, 10)

        // With all three strategies contributing, score should be high
        assertTrue("Combined score should be > 0.5", result.items.first().score > 0.5)
    }
}
