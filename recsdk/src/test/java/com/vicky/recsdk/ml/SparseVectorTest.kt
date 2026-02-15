package com.vicky.recsdk.ml

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class SparseVectorTest {

    @Test
    fun `get and set values`() {
        val v = SparseVector()
        v[0] = 1.0
        v[3] = 2.5
        assertEquals(1.0, v[0], 0.001)
        assertEquals(2.5, v[3], 0.001)
        assertEquals(0.0, v[1], 0.001) // unset index
    }

    @Test
    fun `setting zero removes entry`() {
        val v = SparseVector()
        v[0] = 1.0
        assertEquals(1, v.size)
        v[0] = 0.0
        assertEquals(0, v.size)
    }

    @Test
    fun `dot product of orthogonal vectors is zero`() {
        val a = SparseVector()
        a[0] = 1.0
        val b = SparseVector()
        b[1] = 1.0
        assertEquals(0.0, a.dot(b), 0.001)
    }

    @Test
    fun `dot product of parallel vectors`() {
        val a = SparseVector()
        a[0] = 3.0; a[1] = 4.0
        val b = SparseVector()
        b[0] = 3.0; b[1] = 4.0
        assertEquals(25.0, a.dot(b), 0.001)
    }

    @Test
    fun `dot product with partial overlap`() {
        val a = SparseVector()
        a[0] = 2.0; a[1] = 3.0
        val b = SparseVector()
        b[1] = 4.0; b[2] = 5.0
        assertEquals(12.0, a.dot(b), 0.001) // only index 1 overlaps
    }

    @Test
    fun `norm of vector`() {
        val v = SparseVector()
        v[0] = 3.0; v[1] = 4.0
        assertEquals(5.0, v.norm(), 0.001)
    }

    @Test
    fun `norm of empty vector is zero`() {
        assertEquals(0.0, SparseVector().norm(), 0.001)
    }

    @Test
    fun `cosine similarity of identical vectors is 1`() {
        val a = SparseVector()
        a[0] = 1.0; a[1] = 2.0; a[2] = 3.0
        val b = SparseVector()
        b[0] = 1.0; b[1] = 2.0; b[2] = 3.0
        assertEquals(1.0, a.cosineSimilarity(b), 0.001)
    }

    @Test
    fun `cosine similarity of orthogonal vectors is 0`() {
        val a = SparseVector()
        a[0] = 1.0
        val b = SparseVector()
        b[1] = 1.0
        assertEquals(0.0, a.cosineSimilarity(b), 0.001)
    }

    @Test
    fun `cosine similarity with empty vector is 0`() {
        val a = SparseVector()
        a[0] = 1.0
        assertEquals(0.0, a.cosineSimilarity(SparseVector()), 0.001)
    }

    @Test
    fun `add vectors`() {
        val a = SparseVector()
        a[0] = 1.0; a[1] = 2.0
        val b = SparseVector()
        b[1] = 3.0; b[2] = 4.0
        val result = a.add(b)
        assertEquals(1.0, result[0], 0.001)
        assertEquals(5.0, result[1], 0.001)
        assertEquals(4.0, result[2], 0.001)
    }

    @Test
    fun `scale vector`() {
        val v = SparseVector()
        v[0] = 2.0; v[1] = 3.0
        val scaled = v.scale(0.5)
        assertEquals(1.0, scaled[0], 0.001)
        assertEquals(1.5, scaled[1], 0.001)
    }

    @Test
    fun `scale by zero returns empty vector`() {
        val v = SparseVector()
        v[0] = 2.0
        val scaled = v.scale(0.0)
        assertEquals(0, scaled.size)
    }

    @Test
    fun `json round trip`() {
        val v = SparseVector()
        v[0] = 1.5; v[10] = 2.7; v[100] = 3.9
        val json = v.toJson()
        val restored = SparseVector.fromJson(json)
        assertEquals(1.5, restored[0], 0.001)
        assertEquals(2.7, restored[10], 0.001)
        assertEquals(3.9, restored[100], 0.001)
        assertEquals(3, restored.size)
    }
}
