package com.vicky.recsdk.profile

internal class KeywordClassifier : ItemClassifier {

    private val tagKeywords: Map<InterestTag, Set<String>> = mapOf(
        InterestTag.VEGAN to setOf(
            "vegan", "plant-based", "cruelty-free", "dairy-free",
            "plant protein", "soy", "tofu", "almond milk", "oat milk",
            "vegan leather", "meatless", "no animal"
        ),
        InterestTag.VEGETARIAN to setOf(
            "vegetarian", "veggie", "meat-free", "paneer",
            "cottage cheese", "lentil", "chickpea", "beans"
        ),
        InterestTag.NON_VEG to setOf(
            "chicken", "beef", "pork", "lamb", "mutton", "fish",
            "seafood", "meat", "steak", "bacon", "turkey", "prawn",
            "shrimp", "salmon", "tuna", "crab", "lobster", "sausage",
            "ham", "jerky", "brisket"
        ),
        InterestTag.ORGANIC to setOf(
            "organic", "natural", "chemical-free", "eco-friendly",
            "sustainable", "green", "biodegradable", "pesticide-free",
            "non-gmo", "fair-trade"
        ),
        InterestTag.FITNESS to setOf(
            "fitness", "gym", "workout", "exercise", "protein",
            "supplement", "whey", "dumbbell", "yoga", "running",
            "athletic", "sportswear", "treadmill", "creatine",
            "pre-workout", "resistance band", "kettlebell"
        ),
        InterestTag.BEAUTY to setOf(
            "beauty", "skincare", "cosmetic", "makeup", "serum",
            "moisturizer", "foundation", "lipstick", "mascara",
            "cream", "sunscreen", "cleanser", "toner", "concealer",
            "perfume", "fragrance", "nail polish"
        ),
        InterestTag.TECH to setOf(
            "laptop", "smartphone", "tablet", "phone", "computer",
            "bluetooth", "wireless", "usb", "charger", "electronic",
            "digital", "headphone", "earbuds", "monitor", "keyboard",
            "mouse", "camera", "drone", "smartwatch", "speaker",
            "processor", "gpu", "ssd", "ram"
        ),
        InterestTag.FASHION to setOf(
            "fashion", "dress", "shirt", "shoe", "watch", "handbag",
            "jacket", "jeans", "style", "designer", "sneaker", "boot",
            "sandal", "t-shirt", "hoodie", "sweater", "skirt", "blazer",
            "accessory", "sunglasses", "belt", "scarf"
        ),
        InterestTag.HOME_DECOR to setOf(
            "home", "decor", "furniture", "curtain", "pillow", "lamp",
            "vase", "rug", "carpet", "shelf", "candle", "wall art",
            "cushion", "bedding", "towel", "kitchen"
        ),
        InterestTag.LUXURY to setOf(
            "luxury", "premium", "exclusive", "designer", "gold",
            "diamond", "platinum", "silk", "cashmere", "haute couture",
            "limited edition"
        ),
        InterestTag.SPORTS to setOf(
            "sport", "cricket", "football", "basketball", "tennis",
            "soccer", "baseball", "golf", "swimming", "cycling",
            "hiking", "climbing", "surfing", "skateboard"
        ),
        InterestTag.OUTDOOR to setOf(
            "outdoor", "adventure", "camping", "tent", "backpack",
            "trail", "mountain", "fishing", "hunting", "survival",
            "flashlight", "compass", "binocular"
        ),
        InterestTag.KIDS to setOf(
            "kids", "children", "baby", "toddler", "toy", "diaper",
            "stroller", "nursery", "playmat", "puzzle", "lego",
            "crib", "baby food", "infant"
        ),
        InterestTag.GROCERIES to setOf(
            "grocery", "groceries", "rice", "flour", "sugar", "oil",
            "spice", "masala", "dal", "milk", "bread", "butter",
            "cheese", "egg", "cereal", "pasta", "noodle", "sauce"
        ),
        InterestTag.AUTOMOTIVE to setOf(
            "car", "automotive", "tire", "engine", "motor", "brake",
            "dashboard", "steering", "fuel", "gasoline", "battery",
            "headlight", "wiper", "bumper"
        )
    )

    override fun classify(
        title: String,
        description: String,
        tags: List<String>
    ): Map<String, Double> {
        val text = "$title $description ${tags.joinToString(" ")}".lowercase()
        val words = text.split(Regex("[\\s,;.!?()\\[\\]{}\"']+")).filter { it.isNotBlank() }

        val scores = mutableMapOf<String, Double>()

        for ((tag, keywords) in tagKeywords) {
            val matchCount = words.count { word ->
                keywords.any { keyword ->
                    if (keyword.contains(' ')) {
                        text.contains(keyword)
                    } else {
                        word == keyword || word.startsWith(keyword) || keyword.startsWith(word)
                    }
                }
            }
            if (matchCount > 0) {
                scores[tag.displayName] = minOf(matchCount.toDouble() / 3.0, 1.0)
            }
        }
        return scores
    }
}
