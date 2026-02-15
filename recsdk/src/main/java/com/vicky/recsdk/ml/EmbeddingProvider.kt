package com.vicky.recsdk.ml

/**
 * Public interface for external embedding providers (e.g., TFLite).
 *
 * Implement this to plug in a custom semantic embedding model.
 * The SDK handles all storage, caching, and scoring internally —
 * your provider only needs to convert text to vectors.
 *
 * Example usage:
 * ```kotlin
 * RecoEngine.init(context, RecoConfig(
 *     embeddingProvider = MyTfLiteProvider(context)
 * ))
 * ```
 */
interface EmbeddingProvider {

    /**
     * Generate an embedding vector for the given text.
     * The text is a concatenation of product title, description, category, brand, and tags.
     *
     * @param text Product text to embed
     * @return Dense embedding vector as FloatArray
     */
    fun embedText(text: String): FloatArray

    /**
     * The dimensionality of embedding vectors produced by this provider.
     * For example, Universal Sentence Encoder returns 512.
     */
    fun dimensions(): Int
}
