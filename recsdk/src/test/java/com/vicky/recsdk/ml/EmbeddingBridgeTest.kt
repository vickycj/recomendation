package com.vicky.recsdk.ml

import com.vicky.recsdk.model.RecoItem
import com.vicky.recsdk.storage.entity.EmbeddingEntity
import com.vicky.recsdk.storage.entity.EventEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EmbeddingBridgeTest {

    private lateinit var fakeDao: FakeEmbeddingDao
    private lateinit var fakeProvider: FakeEmbeddingProvider
    private lateinit var bridge: EmbeddingBridge
    private val fakeTime = object : com.vicky.recsdk.util.TimeProvider {
        override fun now(): Long = System.currentTimeMillis()
    }

    @Before
    fun setup() {
        fakeDao = FakeEmbeddingDao()
        fakeProvider = FakeEmbeddingProvider()
        bridge = EmbeddingBridge(fakeProvider, fakeDao, fakeTime)
    }

    private fun createItem(id: String, title: String, description: String = "") = RecoItem(
        id = id, title = title, description = description,
        category = "", brand = "", price = 10.0, imageUrl = ""
    )

    @Test
    fun `buildAndStore calls provider for each item`() = runBlocking {
        val items = listOf(
            createItem("1", "Running Shoes"),
            createItem("2", "Yoga Mat")
        )
        bridge.buildAndStore(items)

        assertEquals(2, fakeProvider.callCount)
        assertEquals(2, fakeDao.stored.size)
    }

    @Test
    fun `buildAndStore with empty list does nothing`() = runBlocking {
        bridge.buildAndStore(emptyList())
        assertEquals(0, fakeProvider.callCount)
    }

    @Test
    fun `getCachedSimilarity returns 0 for unknown item`() = runBlocking {
        bridge.buildAndStore(listOf(createItem("1", "Test")))
        bridge.prepareUserEmbedding(listOf(EventEntity(1, "1", "CLICK", 2.0, 1000L)))
        assertEquals(0.0, bridge.getCachedSimilarity("unknown"), 0.001)
    }

    @Test
    fun `prepareUserEmbedding with empty events resets to zero`() = runBlocking {
        bridge.buildAndStore(listOf(createItem("1", "Test")))
        bridge.prepareUserEmbedding(emptyList())
        assertEquals(0.0, bridge.getCachedSimilarity("1"), 0.001)
    }

    @Test
    fun `similar items get positive similarity after prepare`() = runBlocking {
        // Provider returns unique vectors per text
        fakeProvider.vectorMap["Running Shoes"] = floatArrayOf(1f, 0f, 0f, 0f)
        fakeProvider.vectorMap["Running Sneakers"] = floatArrayOf(0.9f, 0.1f, 0f, 0f)
        fakeProvider.vectorMap["Chocolate Cake"] = floatArrayOf(0f, 0f, 1f, 0f)
        fakeProvider.dims = 4

        // Rebuild bridge with new dimensions
        bridge = EmbeddingBridge(fakeProvider, fakeDao, fakeTime)

        val items = listOf(
            createItem("1", "Running Shoes"),
            createItem("2", "Running Sneakers"),
            createItem("3", "Chocolate Cake")
        )
        bridge.buildAndStore(items)

        // User purchased running shoes
        bridge.prepareUserEmbedding(listOf(
            EventEntity(1, "1", "PURCHASE", 8.0, 1000L)
        ))

        val simToSneakers = bridge.getCachedSimilarity("2")
        val simToCake = bridge.getCachedSimilarity("3")

        assertTrue("Sneakers ($simToSneakers) should be more similar than cake ($simToCake)",
            simToSneakers > simToCake)
    }

    @Test
    fun `heavier events contribute more to user embedding`() = runBlocking {
        fakeProvider.vectorMap["Item A"] = floatArrayOf(1f, 0f)
        fakeProvider.vectorMap["Item B"] = floatArrayOf(0f, 1f)
        fakeProvider.vectorMap["Item C"] = floatArrayOf(1f, 0f) // same direction as A
        fakeProvider.dims = 2
        bridge = EmbeddingBridge(fakeProvider, fakeDao, fakeTime)

        val items = listOf(
            createItem("A", "Item A"),
            createItem("B", "Item B"),
            createItem("C", "Item C")
        )
        bridge.buildAndStore(items)

        // Heavy purchase on A, light view on B
        bridge.prepareUserEmbedding(listOf(
            EventEntity(1, "A", "PURCHASE", 8.0, 1000L),
            EventEntity(2, "B", "VIEW", 1.0, 2000L)
        ))

        val simToC = bridge.getCachedSimilarity("C") // same direction as A
        assertTrue("Item C should have high similarity (same direction as purchased item A)", simToC > 0.5)
    }

    @Test
    fun `clearAll resets all state`() = runBlocking {
        bridge.buildAndStore(listOf(createItem("1", "Test")))
        bridge.prepareUserEmbedding(listOf(EventEntity(1, "1", "CLICK", 2.0, 1000L)))

        bridge.clearAll()

        assertEquals(0.0, bridge.getCachedSimilarity("1"), 0.001)
        assertTrue(fakeDao.stored.isEmpty())
    }

    @Test
    fun `loadPersistedEmbeddings restores from dao`() = runBlocking {
        // Manually store an embedding
        val vector = DenseVector(floatArrayOf(1f, 2f, 3f, 4f))
        fakeDao.stored.add(EmbeddingEntity("item1", vector.toJson(), 1000L))

        fakeProvider.dims = 4
        bridge = EmbeddingBridge(fakeProvider, fakeDao, fakeTime)
        bridge.loadPersistedEmbeddings()

        // Prepare with an event for item1
        bridge.prepareUserEmbedding(listOf(EventEntity(1, "item1", "CLICK", 2.0, 1000L)))

        // Self-similarity should be 1.0
        assertEquals(1.0, bridge.getCachedSimilarity("item1"), 0.001)
    }
}

/**
 * Fake embedding provider for testing.
 */
internal class FakeEmbeddingProvider : EmbeddingProvider {
    var callCount = 0
    var dims = 8
    val vectorMap = mutableMapOf<String, FloatArray>()

    override fun embedText(text: String): FloatArray {
        callCount++
        // Return pre-configured vector if available, otherwise generate based on hash
        vectorMap[text]?.let { return it }

        val hash = text.hashCode()
        return FloatArray(dims) { i ->
            ((hash shr (i % 32)) and 0xFF).toFloat() / 255f
        }
    }

    override fun dimensions(): Int = dims
}
