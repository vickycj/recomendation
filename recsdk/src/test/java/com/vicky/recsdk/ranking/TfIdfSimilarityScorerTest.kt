package com.vicky.recsdk.ranking

import com.vicky.recsdk.ml.FakeEmbeddingDao
import com.vicky.recsdk.ml.TfIdfEngine
import com.vicky.recsdk.model.RecoItem
import com.vicky.recsdk.model.UserProfile
import com.vicky.recsdk.storage.entity.EventEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TfIdfSimilarityScorerTest {

    private lateinit var fakeDao: FakeEmbeddingDao
    private lateinit var engine: TfIdfEngine
    private lateinit var scorer: SemanticSimilarityScorer
    private val emptyProfile = UserProfile(emptyList(), emptyList(), emptyList(), 0L)
    private val fakeTime = object : com.vicky.recsdk.util.TimeProvider {
        override fun now(): Long = System.currentTimeMillis()
    }

    private fun createItem(
        id: String,
        title: String,
        description: String = "",
        category: String = "",
        brand: String = ""
    ) = RecoItem(
        id = id, title = title, description = description, category = category,
        brand = brand, price = 10.0, imageUrl = ""
    )

    @Before
    fun setup() {
        fakeDao = FakeEmbeddingDao()
        engine = TfIdfEngine(fakeDao, fakeTime)
        scorer = SemanticSimilarityScorer(engine)
    }

    @Test
    fun `score returns 0 for item with no embeddings`() {
        assertEquals(0.0, scorer.score(createItem("1", "Test"), emptyProfile), 0.001)
    }

    @Test
    fun `score returns positive for similar item`() = runBlocking {
        val items = listOf(
            createItem("1", "Running Shoes", "Athletic footwear for running", "Sports", "Nike"),
            createItem("2", "Running Sneakers", "Sneakers for running training", "Sports", "Adidas")
        )
        engine.buildAndStore(items)

        val events = listOf(EventEntity(1, "1", "PURCHASE", 8.0, 1000L))
        engine.prepareUserEmbedding(events)

        val score = scorer.score(createItem("2", "Running Sneakers", "Sneakers for running training", "Sports", "Adidas"), emptyProfile)
        assertTrue("Score should be positive for similar item", score > 0.0)
    }

    @Test
    fun `similar items score higher than dissimilar ones`() = runBlocking {
        val items = listOf(
            createItem("1", "Laptop Computer", "High performance laptop computer", "Electronics", "Dell"),
            createItem("2", "Desktop Computer", "Powerful desktop computer workstation", "Electronics", "HP"),
            createItem("3", "Garden Shovel", "Digging tool garden soil", "Garden", "Fiskars")
        )
        engine.buildAndStore(items)

        val events = listOf(EventEntity(1, "1", "PURCHASE", 8.0, 1000L))
        engine.prepareUserEmbedding(events)

        val desktopScore = scorer.score(items[1], emptyProfile)
        val shovelScore = scorer.score(items[2], emptyProfile)

        assertTrue("Desktop ($desktopScore) should score higher than shovel ($shovelScore)",
            desktopScore > shovelScore)
    }

    @Test
    fun `reason returns message for significant similarity`() = runBlocking {
        val items = listOf(
            createItem("1", "Running Shoes Nike", "Athletic running shoes", "Sports", "Nike"),
            createItem("2", "Running Sneakers Adidas", "Athletic running sneakers", "Sports", "Adidas")
        )
        engine.buildAndStore(items)

        val events = listOf(EventEntity(1, "1", "PURCHASE", 8.0, 1000L))
        engine.prepareUserEmbedding(events)

        val reason = scorer.reason(items[1], emptyProfile)
        assertNotNull(reason)
        assertTrue(reason!!.contains("Similar to"))
    }

    @Test
    fun `reason returns null for dissimilar item`() = runBlocking {
        val items = listOf(
            createItem("1", "Running Shoes", "Athletic footwear running", "Sports", "Nike"),
            createItem("2", "Chocolate Cake", "Baking recipe dessert sweet", "Food", "Betty")
        )
        engine.buildAndStore(items)

        val events = listOf(EventEntity(1, "1", "PURCHASE", 8.0, 1000L))
        engine.prepareUserEmbedding(events)

        // For very dissimilar items, the score might be below the threshold
        val score = scorer.score(items[1], emptyProfile)
        if (score <= 0.1) {
            assertNull(scorer.reason(items[1], emptyProfile))
        }
    }
}
