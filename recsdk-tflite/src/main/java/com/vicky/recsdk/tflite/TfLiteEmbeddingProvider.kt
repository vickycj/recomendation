package com.vicky.recsdk.tflite

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.vicky.recsdk.ml.EmbeddingProvider

/**
 * TFLite-based [EmbeddingProvider] using MediaPipe's Universal Sentence Encoder.
 *
 * Converts product text into 512-dimensional dense embeddings that capture
 * semantic meaning — so "running shoes" and "jogging sneakers" produce
 * similar vectors even without shared keywords.
 *
 * Usage:
 * ```kotlin
 * val provider = TfLiteEmbeddingProvider(context)
 * RecoEngine.init(context, RecoConfig(
 *     embeddingProvider = provider
 * ))
 * ```
 *
 * @param context Android context for loading the bundled model asset
 * @param modelPath Asset path to the USE .tflite model (default: bundled model)
 */
class TfLiteEmbeddingProvider(
    context: Context,
    modelPath: String = DEFAULT_MODEL_PATH
) : EmbeddingProvider {

    private val embedder: TextEmbedder
    private val detectedDimensions: Int

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(modelPath)
            .build()

        val options = TextEmbedder.TextEmbedderOptions.builder()
            .setBaseOptions(baseOptions)
            .build()

        embedder = TextEmbedder.createFromOptions(context, options)

        // Detect actual model dimensions by running a probe embedding
        val probe = embedder.embed("test")
        detectedDimensions = probe.embeddingResult().embeddings()[0].floatEmbedding().size
    }

    override fun embedText(text: String): FloatArray {
        if (text.isBlank()) {
            return FloatArray(detectedDimensions) { 0f }
        }

        // Truncate to stay within model limits
        val truncated = if (text.length > 512) text.take(512) else text

        val result = embedder.embed(truncated)
        val embedding = result.embeddingResult().embeddings()[0]
        val floatBuffer = embedding.floatEmbedding()

        return FloatArray(floatBuffer.size) { i -> floatBuffer[i] }
    }

    override fun dimensions(): Int = detectedDimensions

    /**
     * Release native resources held by the MediaPipe embedder.
     * Call this when the provider is no longer needed.
     */
    fun close() {
        embedder.close()
    }

    companion object {
        private const val DEFAULT_MODEL_PATH = "universal_sentence_encoder.tflite"
    }
}
