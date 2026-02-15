package com.vicky.recsdk.ml

import org.junit.Assert.*
import org.junit.Test

class DenseVectorTest {

    @Test
    fun `dot product of identical vectors`() {
        val a = DenseVector(floatArrayOf(3f, 4f))
        assertEquals(25f, a.dot(a), 0.001f)
    }

    @Test
    fun `dot product of orthogonal vectors is zero`() {
        val a = DenseVector(floatArrayOf(1f, 0f))
        val b = DenseVector(floatArrayOf(0f, 1f))
        assertEquals(0f, a.dot(b), 0.001f)
    }

    @Test
    fun `norm of 3-4-5 triangle`() {
        val v = DenseVector(floatArrayOf(3f, 4f))
        assertEquals(5f, v.norm(), 0.001f)
    }

    @Test
    fun `norm of zero vector is zero`() {
        assertEquals(0f, DenseVector.zero(3).norm(), 0.001f)
    }

    @Test
    fun `cosine similarity of identical vectors is 1`() {
        val a = DenseVector(floatArrayOf(1f, 2f, 3f))
        assertEquals(1.0, a.cosineSimilarity(a), 0.001)
    }

    @Test
    fun `cosine similarity of orthogonal vectors is 0`() {
        val a = DenseVector(floatArrayOf(1f, 0f))
        val b = DenseVector(floatArrayOf(0f, 1f))
        assertEquals(0.0, a.cosineSimilarity(b), 0.001)
    }

    @Test
    fun `cosine similarity with zero vector is 0`() {
        val a = DenseVector(floatArrayOf(1f, 2f))
        val b = DenseVector.zero(2)
        assertEquals(0.0, a.cosineSimilarity(b), 0.001)
    }

    @Test
    fun `cosine similarity of opposite vectors is -1`() {
        val a = DenseVector(floatArrayOf(1f, 0f))
        val b = DenseVector(floatArrayOf(-1f, 0f))
        assertEquals(-1.0, a.cosineSimilarity(b), 0.001)
    }

    @Test
    fun `add vectors`() {
        val a = DenseVector(floatArrayOf(1f, 2f, 3f))
        val b = DenseVector(floatArrayOf(4f, 5f, 6f))
        val result = a.add(b)
        assertArrayEquals(floatArrayOf(5f, 7f, 9f), result.values, 0.001f)
    }

    @Test
    fun `scale vector`() {
        val v = DenseVector(floatArrayOf(2f, 4f))
        val scaled = v.scale(0.5f)
        assertArrayEquals(floatArrayOf(1f, 2f), scaled.values, 0.001f)
    }

    @Test
    fun `scale by zero returns zero vector`() {
        val v = DenseVector(floatArrayOf(1f, 2f, 3f))
        val scaled = v.scale(0f)
        for (value in scaled.values) {
            assertEquals(0f, value, 0.001f)
        }
    }

    @Test
    fun `json round trip`() {
        val v = DenseVector(floatArrayOf(1.5f, 2.7f, 3.9f))
        val json = v.toJson()
        val restored = DenseVector.fromJson(json)
        assertArrayEquals(v.values, restored.values, 0.001f)
    }

    @Test
    fun `zero factory creates zero vector`() {
        val v = DenseVector.zero(512)
        assertEquals(512, v.size)
        for (value in v.values) {
            assertEquals(0f, value, 0.001f)
        }
    }

    @Test
    fun `equals works with content equality`() {
        val a = DenseVector(floatArrayOf(1f, 2f))
        val b = DenseVector(floatArrayOf(1f, 2f))
        assertEquals(a, b)
    }

    @Test
    fun `dimension mismatch returns zero for dot`() {
        val a = DenseVector(floatArrayOf(1f, 2f))
        val b = DenseVector(floatArrayOf(1f, 2f, 3f))
        assertEquals(0f, a.dot(b), 0.001f)
    }

    @Test
    fun `dimension mismatch returns zero for cosine similarity`() {
        val a = DenseVector(floatArrayOf(1f, 2f))
        val b = DenseVector(floatArrayOf(1f, 2f, 3f))
        assertEquals(0.0, a.cosineSimilarity(b), 0.001)
    }

    @Test
    fun `dimension mismatch add returns the non-zero vector`() {
        val zero = DenseVector.zero(2)
        val nonZero = DenseVector(floatArrayOf(1f, 2f, 3f))
        val result = zero.add(nonZero)
        assertArrayEquals(nonZero.values, result.values, 0.001f)
    }
}
