package com.vicky.recsdk.tflite

import com.vicky.recsdk.ml.EmbeddingProvider
import com.vicky.recsdk.profile.ItemClassifier

/**
 * Semantic [ItemClassifier] that uses embedding similarity for tag classification.
 *
 * Instead of keyword matching, this computes cosine similarity between product
 * text embeddings and pre-defined category embeddings. This means "organic quinoa
 * bowl" can be classified as "Vegan" without containing the word "vegan".
 *
 * Usage:
 * ```kotlin
 * val provider = TfLiteEmbeddingProvider(context)
 * val classifier = SemanticClassifier(provider)
 * val tags = classifier.classify("Organic Quinoa Bowl", "A healthy plant-based meal", listOf())
 * // tags might be: {"Vegan" to 0.82, "Organic" to 0.75, "Healthy" to 0.71}
 * ```
 *
 * @param provider The embedding provider to use for vectorizing text
 * @param threshold Minimum similarity score to include a tag (default: 0.3)
 */
class SemanticClassifier(
    private val provider: EmbeddingProvider,
    private val threshold: Double = DEFAULT_THRESHOLD
) : ItemClassifier {

    // Pre-computed category embeddings (lazily initialized)
    private val categoryEmbeddings: Map<String, FloatArray> by lazy {
        CATEGORY_DESCRIPTIONS.mapValues { (_, description) ->
            provider.embedText(description)
        }
    }

    override fun classify(
        title: String,
        description: String,
        tags: List<String>
    ): Map<String, Double> {
        val combinedText = buildString {
            append(title)
            if (description.isNotBlank()) {
                append(" ")
                append(description)
            }
            if (tags.isNotEmpty()) {
                append(" ")
                append(tags.joinToString(" "))
            }
        }

        if (combinedText.isBlank()) return emptyMap()

        val itemEmbedding = provider.embedText(combinedText)
        val results = mutableMapOf<String, Double>()

        for ((tag, categoryEmbedding) in categoryEmbeddings) {
            val similarity = cosineSimilarity(itemEmbedding, categoryEmbedding)
            if (similarity >= threshold) {
                results[tag] = similarity
            }
        }

        return results
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        if (a.size != b.size) return 0.0

        var dot = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        val denom = Math.sqrt(normA) * Math.sqrt(normB)
        return if (denom > 0.0) dot / denom else 0.0
    }

    companion object {
        private const val DEFAULT_THRESHOLD = 0.3

        /**
         * Category descriptions used to generate reference embeddings.
         * Each description captures the semantic meaning of the category
         * so the model can match products based on meaning, not keywords.
         */
        internal val CATEGORY_DESCRIPTIONS = mapOf(
            "Vegan" to "vegan plant-based food without animal products dairy-free meat-free",
            "Vegetarian" to "vegetarian food without meat but may include dairy eggs cheese",
            "Non-Vegetarian" to "meat chicken fish seafood pork beef lamb non-vegetarian",
            "Organic" to "organic natural chemical-free pesticide-free sustainable farming",
            "Gluten-Free" to "gluten-free celiac wheat-free grain-free no gluten",
            "Healthy" to "healthy nutritious low-calorie diet fitness wellness superfood",
            "Premium" to "premium luxury high-end expensive gourmet artisan exclusive",
            "Budget" to "budget affordable cheap value economy low-cost bargain discount",
            "Spicy" to "spicy hot chili pepper jalapeño cayenne sriracha wasabi",
            "Sweet" to "sweet dessert candy chocolate cake sugar honey pastry",
            "Beverage" to "drink beverage juice smoothie coffee tea water soda",
            "Snack" to "snack chips crackers nuts popcorn cookie granola bar",
            "Breakfast" to "breakfast cereal pancake waffle oatmeal eggs toast morning meal",
            "Baby" to "baby infant toddler child kids formula diapers",
            "Pet" to "pet dog cat animal food treats toys pet care",
            "Household" to "household cleaning laundry detergent kitchen bathroom supplies",
            "Personal Care" to "personal care hygiene skincare shampoo soap toothpaste beauty"
        )
    }
}
