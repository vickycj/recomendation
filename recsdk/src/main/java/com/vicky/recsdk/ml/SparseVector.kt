package com.vicky.recsdk.ml

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.sqrt

internal class SparseVector(
    private val entries: MutableMap<Int, Double> = mutableMapOf()
) {

    operator fun get(index: Int): Double = entries.getOrDefault(index, 0.0)

    operator fun set(index: Int, value: Double) {
        if (value == 0.0) {
            entries.remove(index)
        } else {
            entries[index] = value
        }
    }

    val size: Int get() = entries.size

    val indices: Set<Int> get() = entries.keys

    fun dot(other: SparseVector): Double {
        val smaller = if (this.size <= other.size) this else other
        val larger = if (this.size <= other.size) other else this
        return smaller.entries.entries.sumOf { (index, value) ->
            value * larger[index]
        }
    }

    fun norm(): Double = sqrt(entries.values.sumOf { it * it })

    fun cosineSimilarity(other: SparseVector): Double {
        val normA = norm()
        val normB = other.norm()
        if (normA == 0.0 || normB == 0.0) return 0.0
        return dot(other) / (normA * normB)
    }

    fun add(other: SparseVector): SparseVector {
        val result = SparseVector(entries.toMutableMap())
        for ((index, value) in other.entries) {
            result[index] = result[index] + value
        }
        return result
    }

    fun scale(factor: Double): SparseVector {
        if (factor == 0.0) return SparseVector()
        val result = SparseVector()
        for ((index, value) in entries) {
            result[index] = value * factor
        }
        return result
    }

    fun toJson(): String = Gson().toJson(entries)

    companion object {
        fun fromJson(json: String): SparseVector {
            val type = object : TypeToken<Map<String, Double>>() {}.type
            val map: Map<String, Double> = Gson().fromJson(json, type)
            val entries = mutableMapOf<Int, Double>()
            for ((key, value) in map) {
                entries[key.toInt()] = value
            }
            return SparseVector(entries)
        }
    }
}
