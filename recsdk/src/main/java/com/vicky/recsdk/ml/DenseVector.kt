package com.vicky.recsdk.ml

import com.google.gson.Gson
import kotlin.math.sqrt

/**
 * Dense vector representation for semantic embeddings (e.g., TFLite).
 * More memory-efficient than SparseVector for high-dimensional dense embeddings.
 */
internal class DenseVector(
    val values: FloatArray
) {

    val size: Int get() = values.size

    fun dot(other: DenseVector): Float {
        if (values.size != other.values.size) return 0f
        var sum = 0f
        for (i in values.indices) {
            sum += values[i] * other.values[i]
        }
        return sum
    }

    fun norm(): Float {
        var sum = 0f
        for (v in values) {
            sum += v * v
        }
        return sqrt(sum)
    }

    fun cosineSimilarity(other: DenseVector): Double {
        if (values.size != other.values.size) return 0.0
        val normA = norm()
        val normB = other.norm()
        if (normA == 0f || normB == 0f) return 0.0
        return (dot(other) / (normA * normB)).toDouble()
    }

    fun add(other: DenseVector): DenseVector {
        if (values.size != other.values.size) {
            // Return the non-zero vector when dimensions mismatch (e.g., during init)
            return if (other.norm() > 0f) other else this
        }
        val result = FloatArray(values.size)
        for (i in values.indices) {
            result[i] = values[i] + other.values[i]
        }
        return DenseVector(result)
    }

    fun scale(factor: Float): DenseVector {
        val result = FloatArray(values.size)
        for (i in values.indices) {
            result[i] = values[i] * factor
        }
        return DenseVector(result)
    }

    fun toJson(): String = Gson().toJson(values)

    companion object {
        fun fromJson(json: String): DenseVector {
            val array = Gson().fromJson(json, FloatArray::class.java)
            return DenseVector(array)
        }

        fun zero(dimensions: Int): DenseVector = DenseVector(FloatArray(dimensions))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DenseVector) return false
        return values.contentEquals(other.values)
    }

    override fun hashCode(): Int = values.contentHashCode()
}
