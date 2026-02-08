package com.vicky.recsdk.profile

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class KeywordClassifierTest {

    private lateinit var classifier: KeywordClassifier

    @Before
    fun setUp() {
        classifier = KeywordClassifier()
    }

    @Test
    fun `vegan product is classified correctly`() {
        val result = classifier.classify(
            title = "Organic Vegan Protein Bar",
            description = "Plant-based protein bar, dairy-free, cruelty-free",
            tags = listOf("vegan", "protein")
        )
        assertTrue("Should contain Vegan tag", result.containsKey("Vegan"))
        assertTrue("Vegan score should be > 0.5", result["Vegan"]!! > 0.5)
    }

    @Test
    fun `mutton product classified as Non-Vegetarian`() {
        val result = classifier.classify(
            title = "Premium Mutton Curry Mix",
            description = "Authentic lamb and mutton spice blend for meat lovers",
            tags = listOf("meat", "spices")
        )
        assertTrue("Should contain Non-Vegetarian tag", result.containsKey("Non-Vegetarian"))
    }

    @Test
    fun `fitness product classified correctly`() {
        val result = classifier.classify(
            title = "Whey Protein Supplement",
            description = "Premium whey protein for gym workout and fitness recovery",
            tags = listOf("fitness", "protein", "supplement")
        )
        assertTrue("Should contain Fitness tag", result.containsKey("Fitness"))
        assertTrue("Fitness score should be high", result["Fitness"]!! >= 0.67)
    }

    @Test
    fun `tech product classified correctly`() {
        val result = classifier.classify(
            title = "Wireless Bluetooth Headphone",
            description = "Premium wireless bluetooth headphone with USB-C charger",
            tags = listOf("headphone", "wireless", "bluetooth")
        )
        assertTrue("Should contain Tech & Electronics tag",
            result.containsKey("Tech & Electronics"))
    }

    @Test
    fun `beauty product classified correctly`() {
        val result = classifier.classify(
            title = "Anti-Aging Skincare Serum",
            description = "Premium beauty skincare moisturizer serum",
            tags = listOf("beauty", "skincare")
        )
        assertTrue("Should contain Beauty & Skincare tag",
            result.containsKey("Beauty & Skincare"))
    }

    @Test
    fun `fashion product classified correctly`() {
        val result = classifier.classify(
            title = "Designer Leather Jacket",
            description = "Stylish designer fashion jacket",
            tags = listOf("fashion", "jacket")
        )
        assertTrue("Should contain Fashion tag", result.containsKey("Fashion"))
    }

    @Test
    fun `product with no matching keywords returns empty`() {
        val result = classifier.classify(
            title = "ZZZ Abstract Widget QRX",
            description = "Lorem ipsum dolor amet consectetur adipiscing elit",
            tags = emptyList()
        )
        assertTrue("Should be empty, but got: $result", result.isEmpty())
    }

    @Test
    fun `classification is case insensitive`() {
        val result = classifier.classify(
            title = "VEGAN PROTEIN BAR",
            description = "PLANT-BASED DAIRY-FREE CRUELTY-FREE",
            tags = listOf("VEGAN")
        )
        assertTrue("Should detect vegan despite uppercase", result.containsKey("Vegan"))
    }

    @Test
    fun `multiple tags can be detected in one product`() {
        val result = classifier.classify(
            title = "Organic Vegan Fitness Protein Bar",
            description = "Plant-based protein supplement for gym workout, organic natural",
            tags = listOf("vegan", "fitness", "organic")
        )
        // Should detect multiple interests
        val tagCount = result.size
        assertTrue("Should detect multiple tags, found $tagCount", tagCount >= 2)
    }

    @Test
    fun `confidence is capped at 1_0`() {
        val result = classifier.classify(
            title = "vegan vegan vegan vegan vegan",
            description = "vegan plant-based dairy-free cruelty-free tofu soy almond milk oat milk",
            tags = listOf("vegan", "vegan", "vegan")
        )
        assertTrue("Confidence should be capped at 1.0",
            result.values.all { it <= 1.0 })
    }

    @Test
    fun `sports classification works`() {
        val result = classifier.classify(
            title = "Cricket Bat Professional",
            description = "Professional cricket bat for sport enthusiasts, tennis ball compatible",
            tags = listOf("sport", "cricket")
        )
        assertTrue("Should contain Sports tag", result.containsKey("Sports"))
    }

    @Test
    fun `kids product classified correctly`() {
        val result = classifier.classify(
            title = "Baby Stroller Travel System",
            description = "Premium baby stroller for toddler kids, with toy holder",
            tags = listOf("baby", "kids", "stroller")
        )
        assertTrue("Should contain Kids & Family tag",
            result.containsKey("Kids & Family"))
    }
}
