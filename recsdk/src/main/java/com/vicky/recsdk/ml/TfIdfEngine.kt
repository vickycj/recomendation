package com.vicky.recsdk.ml

import com.vicky.recsdk.model.RecoItem
import com.vicky.recsdk.storage.dao.EmbeddingDao
import com.vicky.recsdk.storage.entity.EmbeddingEntity
import com.vicky.recsdk.storage.entity.EventEntity
import com.vicky.recsdk.util.TimeProvider
import kotlin.math.ln

/**
 * TF-IDF engine for computing item text embeddings and user preference embeddings.
 * Uses sparse vectors for memory efficiency. No external ML model needed.
 */
internal class TfIdfEngine(
    private val embeddingDao: EmbeddingDao,
    private val timeProvider: TimeProvider
) : SimilarityEngine {

    // Vocabulary: word -> index
    private var vocabulary: Map<String, Int> = emptyMap()
    // IDF values per vocabulary index
    private var idfValues: Map<Int, Double> = emptyMap()
    // Cached item embeddings
    private var itemEmbeddings: Map<String, SparseVector> = emptyMap()
    // Cached user embedding
    @Volatile
    private var cachedUserEmbedding: SparseVector = SparseVector()

    /**
     * Build vocabulary and compute embeddings for all items.
     * Call this after feedItems().
     */
    override suspend fun buildAndStore(items: List<RecoItem>) {
        if (items.isEmpty()) return

        // Step 1: Tokenize all items and build vocabulary
        val allTokens = items.map { tokenize(it) }
        val wordSet = mutableSetOf<String>()
        allTokens.forEach { wordSet.addAll(it) }
        vocabulary = wordSet.toList().sorted().mapIndexed { index, word -> word to index }.toMap()

        // Step 2: Compute IDF for each word
        val docCount = items.size.toDouble()
        val documentFrequency = mutableMapOf<String, Int>()
        for (tokens in allTokens) {
            val uniqueTokens = tokens.toSet()
            for (token in uniqueTokens) {
                documentFrequency[token] = (documentFrequency[token] ?: 0) + 1
            }
        }
        idfValues = vocabulary.map { (word, index) ->
            val df = documentFrequency[word] ?: 1
            index to ln((docCount + 1) / (df + 1)) + 1.0 // smoothed IDF
        }.toMap()

        // Step 3: Compute TF-IDF vectors per item
        val now = timeProvider.now()
        val embeddings = mutableMapOf<String, SparseVector>()
        val entities = mutableListOf<EmbeddingEntity>()

        for (i in items.indices) {
            val vector = computeTfIdf(allTokens[i])
            embeddings[items[i].id] = vector
            entities.add(EmbeddingEntity(items[i].id, vector.toJson(), now))
        }

        itemEmbeddings = embeddings

        // Persist
        embeddingDao.deleteAll()
        embeddingDao.insertAll(entities)
    }

    /**
     * Compute user embedding as weighted average of interacted item embeddings.
     * Event weight determines how much each item contributes.
     */
    fun computeUserEmbedding(events: List<EventEntity>): SparseVector {
        if (events.isEmpty() || itemEmbeddings.isEmpty()) return SparseVector()

        var combined = SparseVector()
        var totalWeight = 0.0

        for (event in events) {
            val itemVector = itemEmbeddings[event.itemId] ?: continue
            combined = combined.add(itemVector.scale(event.eventWeight))
            totalWeight += event.eventWeight
        }

        return if (totalWeight > 0.0) combined.scale(1.0 / totalWeight) else SparseVector()
    }

    /**
     * Prepare user embedding for synchronous scoring calls.
     */
    override fun prepareUserEmbedding(events: List<EventEntity>) {
        cachedUserEmbedding = computeUserEmbedding(events)
    }

    /**
     * Get cosine similarity between cached user embedding and an item.
     */
    override fun getCachedSimilarity(itemId: String): Double {
        val itemVector = itemEmbeddings[itemId] ?: return 0.0
        return cachedUserEmbedding.cosineSimilarity(itemVector)
    }

    /**
     * Get the item embedding for a specific item.
     */
    fun getItemEmbedding(itemId: String): SparseVector? = itemEmbeddings[itemId]

    /**
     * Load persisted embeddings from Room.
     */
    override suspend fun loadPersistedEmbeddings() {
        val entities = embeddingDao.getAllEmbeddings()
        if (entities.isEmpty()) return
        val loaded = mutableMapOf<String, SparseVector>()
        for (entity in entities) {
            try {
                loaded[entity.itemId] = SparseVector.fromJson(entity.vectorJson)
            } catch (_: Exception) {
                // Skip corrupt entries
            }
        }
        itemEmbeddings = loaded
    }

    override suspend fun clearAll() {
        embeddingDao.deleteAll()
        vocabulary = emptyMap()
        idfValues = emptyMap()
        itemEmbeddings = emptyMap()
        cachedUserEmbedding = SparseVector()
    }

    // -- Internal helpers --

    internal fun tokenize(item: RecoItem): List<String> {
        val text = buildString {
            append(item.title.lowercase())
            append(" ")
            append(item.description.lowercase())
            append(" ")
            append(item.category.lowercase())
            append(" ")
            append(item.brand.lowercase())
            append(" ")
            item.tags.forEach { append(it.lowercase()); append(" ") }
        }
        return text.split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 2 } // skip single chars
    }

    internal fun computeTfIdf(tokens: List<String>): SparseVector {
        val vector = SparseVector()
        if (tokens.isEmpty()) return vector

        // Term frequency
        val tf = mutableMapOf<String, Int>()
        for (token in tokens) {
            tf[token] = (tf[token] ?: 0) + 1
        }

        val maxTf = tf.values.maxOrNull()?.toDouble() ?: 1.0

        for ((word, count) in tf) {
            val index = vocabulary[word] ?: continue
            val normalizedTf = count.toDouble() / maxTf
            val idf = idfValues[index] ?: 1.0
            vector[index] = normalizedTf * idf
        }

        return vector
    }
}
