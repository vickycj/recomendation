package com.vicky.recsdk.profile

/**
 * Interface for classifying product items into interest tags.
 * Default implementation is [KeywordClassifier].
 * Clients can provide a custom implementation (e.g., TFLite-based)
 * via [com.vicky.recsdk.RecoEngine.init].
 */
interface ItemClassifier {

    /**
     * Classify a product based on its text content.
     *
     * @param title Product title
     * @param description Product description
     * @param tags Client-provided product tags
     * @return Map of tag display name to confidence score (0.0 to 1.0)
     */
    fun classify(title: String, description: String, tags: List<String>): Map<String, Double>
}
