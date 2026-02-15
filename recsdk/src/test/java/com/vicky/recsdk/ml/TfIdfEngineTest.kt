package com.vicky.recsdk.ml

import com.vicky.recsdk.model.RecoItem
import com.vicky.recsdk.storage.entity.EmbeddingEntity
import com.vicky.recsdk.storage.entity.EventEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TfIdfEngineTest {

    private lateinit var fakeDao: FakeEmbeddingDao
    private lateinit var engine: TfIdfEngine
    private val fakeTime = object : com.vicky.recsdk.util.TimeProvider {
        override fun now(): Long = System.currentTimeMillis()
    }

    @Before
    fun setup() {
        fakeDao = FakeEmbeddingDao()
        engine = TfIdfEngine(fakeDao, fakeTime)
    }

    private fun createItem(
        id: String,
        title: String,
        description: String = "",
        category: String = "",
        brand: String = "",
        tags: List<String> = emptyList()
    ) = RecoItem(
        id = id,
        title = title,
        description = description,
        category = category,
        brand = brand,
        price = 10.0,
        imageUrl = "",
        tags = tags
    )

    @Test
    fun `tokenize extracts words from all fields`() {
        val item = createItem("1", "iPhone Pro", "Best smartphone ever", "Electronics", "Apple", listOf("phone", "tech"))
        val tokens = engine.tokenize(item)
        assertTrue(tokens.contains("iphone"))
        assertTrue(tokens.contains("pro"))
        assertTrue(tokens.contains("smartphone"))
        assertTrue(tokens.contains("electronics"))
        assertTrue(tokens.contains("apple"))
        assertTrue(tokens.contains("phone"))
        assertTrue(tokens.contains("tech"))
    }

    @Test
    fun `tokenize filters single-char words`() {
        val item = createItem("1", "A B Big item", "x y description")
        val tokens = engine.tokenize(item)
        assertFalse(tokens.contains("a"))
        assertFalse(tokens.contains("b"))
        assertFalse(tokens.contains("x"))
        assertFalse(tokens.contains("y"))
        assertTrue(tokens.contains("big"))
    }

    @Test
    fun `buildAndStore creates embeddings for all items`() = runBlocking {
        val items = listOf(
            createItem("1", "Running Shoes", "Great for jogging and marathon", "Sports", "Nike"),
            createItem("2", "Yoga Mat", "Perfect for yoga and pilates", "Fitness", "Lululemon")
        )
        engine.buildAndStore(items)

        // Check embeddings were stored
        assertEquals(2, fakeDao.stored.size)
        assertNotNull(engine.getItemEmbedding("1"))
        assertNotNull(engine.getItemEmbedding("2"))
    }

    @Test
    fun `similar items have higher cosine similarity`() = runBlocking {
        val items = listOf(
            createItem("1", "Running Shoes Nike", "Great running shoes for jogging marathon fitness", "Sports", "Nike"),
            createItem("2", "Running Sneakers Adidas", "Sports running shoes for training fitness", "Sports", "Adidas"),
            createItem("3", "Chocolate Cake Recipe", "Delicious chocolate cake baking dessert sweet", "Food", "Betty")
        )
        engine.buildAndStore(items)

        val emb1 = engine.getItemEmbedding("1")!!
        val emb2 = engine.getItemEmbedding("2")!!
        val emb3 = engine.getItemEmbedding("3")!!

        val sim12 = emb1.cosineSimilarity(emb2) // both about running shoes
        val sim13 = emb1.cosineSimilarity(emb3) // running vs cake

        assertTrue("Running shoes should be more similar to each other ($sim12) than to cake ($sim13)",
            sim12 > sim13)
    }

    @Test
    fun `computeUserEmbedding produces weighted average`() = runBlocking {
        val items = listOf(
            createItem("1", "Running Shoes", "Sports fitness", "Sports", "Nike"),
            createItem("2", "Yoga Mat", "Fitness wellness", "Fitness", "Lululemon")
        )
        engine.buildAndStore(items)

        val events = listOf(
            EventEntity(1, "1", "PURCHASE", 8.0, 1000L), // heavy weight on running
            EventEntity(2, "2", "VIEW", 1.0, 2000L)       // light weight on yoga
        )

        val userEmb = engine.computeUserEmbedding(events)
        val emb1 = engine.getItemEmbedding("1")!!
        val emb2 = engine.getItemEmbedding("2")!!

        // User embedding should be more similar to item1 (purchased) than item2 (viewed)
        val simToRunning = userEmb.cosineSimilarity(emb1)
        val simToYoga = userEmb.cosineSimilarity(emb2)
        assertTrue("User should prefer running ($simToRunning) over yoga ($simToYoga)",
            simToRunning > simToYoga)
    }

    @Test
    fun `computeUserEmbedding with empty events returns empty vector`() = runBlocking {
        engine.buildAndStore(listOf(createItem("1", "Test", "desc")))
        val emb = engine.computeUserEmbedding(emptyList())
        assertEquals(0, emb.size)
    }

    @Test
    fun `prepareUserEmbedding and getCachedSimilarity work together`() = runBlocking {
        val items = listOf(
            createItem("1", "Laptop Computer", "High performance laptop", "Electronics", "Dell"),
            createItem("2", "Desktop Computer", "Powerful desktop workstation", "Electronics", "HP"),
            createItem("3", "Garden Shovel", "Digging tool for garden", "Garden", "Fiskars")
        )
        engine.buildAndStore(items)

        val events = listOf(
            EventEntity(1, "1", "PURCHASE", 8.0, 1000L)
        )
        engine.prepareUserEmbedding(events)

        val simToDesktop = engine.getCachedSimilarity("2")
        val simToShovel = engine.getCachedSimilarity("3")

        assertTrue("Desktop ($simToDesktop) should be more relevant than shovel ($simToShovel)",
            simToDesktop > simToShovel)
    }

    @Test
    fun `getCachedSimilarity returns 0 for unknown item`() = runBlocking {
        engine.buildAndStore(listOf(createItem("1", "Test")))
        engine.prepareUserEmbedding(listOf(EventEntity(1, "1", "CLICK", 2.0, 1000L)))
        assertEquals(0.0, engine.getCachedSimilarity("unknown"), 0.001)
    }

    @Test
    fun `clearAll resets all state`() = runBlocking {
        engine.buildAndStore(listOf(createItem("1", "Test", "description")))
        assertNotNull(engine.getItemEmbedding("1"))

        engine.clearAll()
        assertNull(engine.getItemEmbedding("1"))
        assertTrue(fakeDao.stored.isEmpty())
    }

    @Test
    fun `empty items list does nothing`() = runBlocking {
        engine.buildAndStore(emptyList())
        assertTrue(fakeDao.stored.isEmpty())
    }
}

/**
 * Fake DAO for testing without Room.
 */
internal class FakeEmbeddingDao : com.vicky.recsdk.storage.dao.EmbeddingDao {
    val stored = mutableListOf<EmbeddingEntity>()

    override suspend fun insertOrUpdate(entity: EmbeddingEntity) {
        stored.removeAll { it.itemId == entity.itemId }
        stored.add(entity)
    }

    override suspend fun insertAll(entities: List<EmbeddingEntity>) {
        for (entity in entities) {
            stored.removeAll { it.itemId == entity.itemId }
        }
        stored.addAll(entities)
    }

    override suspend fun getEmbedding(itemId: String): EmbeddingEntity? {
        return stored.find { it.itemId == itemId }
    }

    override suspend fun getAllEmbeddings(): List<EmbeddingEntity> = stored.toList()

    override suspend fun getEmbeddings(itemIds: List<String>): List<EmbeddingEntity> {
        return stored.filter { it.itemId in itemIds }
    }

    override suspend fun deleteAll() {
        stored.clear()
    }
}
