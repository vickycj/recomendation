package com.vicky.recsdk.profile

import com.google.gson.Gson
import com.vicky.recsdk.model.BrandAffinity
import com.vicky.recsdk.model.CategoryAffinity
import com.vicky.recsdk.model.InterestTagScore
import com.vicky.recsdk.model.UserProfile
import com.vicky.recsdk.storage.dao.EventDao
import com.vicky.recsdk.storage.dao.ItemCacheDao
import com.vicky.recsdk.storage.dao.ProfileDao
import com.vicky.recsdk.storage.entity.ProfileEntity
import com.vicky.recsdk.tracking.BehaviorScorer
import com.vicky.recsdk.util.TimeProvider

internal class ProfileBuilder(
    private val behaviorScorer: BehaviorScorer,
    private val classifier: ItemClassifier,
    private val eventDao: EventDao,
    private val itemCacheDao: ItemCacheDao,
    private val profileDao: ProfileDao,
    private val timeProvider: TimeProvider,
    private val enableTextClassification: Boolean = true
) {

    @Volatile
    private var cachedProfile: UserProfile? = null
    private var lastBuildTime: Long = 0
    private val gson = Gson()

    suspend fun getOrBuildProfile(rebuildIntervalMs: Long): UserProfile {
        val cached = cachedProfile
        if (cached != null && timeProvider.now() - lastBuildTime < rebuildIntervalMs) {
            return cached
        }
        return buildProfile()
    }

    suspend fun invalidateCache() {
        cachedProfile = null
        lastBuildTime = 0
    }

    suspend fun buildProfile(): UserProfile {
        val events = eventDao.getAllEvents()
        if (events.isEmpty()) {
            val emptyProfile = UserProfile(lastUpdated = timeProvider.now())
            cacheAndPersist(emptyProfile)
            return emptyProfile
        }

        val items = itemCacheDao.getAllItems()

        // Category affinity
        val itemCategoryMap = items.associate { it.itemId to it.category }
        val categoryScores = behaviorScorer.computeCategoryScores(events, itemCategoryMap)

        // Brand affinity
        val itemBrandMap = items.associate { it.itemId to it.brand }
        val brandScores = behaviorScorer.computeBrandScores(events, itemBrandMap)

        // Interest tags via text classification
        val aggregatedTags = mutableMapOf<String, Double>()

        if (enableTextClassification) {
            val interactedItemIds = events.map { it.itemId }.toSet()
            val interactedItems = items.filter { it.itemId in interactedItemIds }

            for (item in interactedItems) {
                val itemEvents = events.filter { it.itemId == item.itemId }
                val itemBehaviorScore = behaviorScorer.computeItemScore(itemEvents)

                val itemTags = parseTags(item.tags)
                val textTags = classifier.classify(item.title, item.description, itemTags)

                for ((tag, textConfidence) in textTags) {
                    val combined = textConfidence * itemBehaviorScore
                    aggregatedTags[tag] = (aggregatedTags[tag] ?: 0.0) + combined
                }
            }
        }

        // Normalize tag scores
        val maxTagScore = aggregatedTags.values.maxOrNull() ?: 1.0
        val normalizedTags = if (maxTagScore > 0) {
            aggregatedTags.mapValues { it.value / maxTagScore }
        } else {
            aggregatedTags
        }

        val profile = UserProfile(
            topCategories = categoryScores.entries
                .sortedByDescending { it.value }
                .take(10)
                .map { CategoryAffinity(it.key, it.value) },
            topBrands = brandScores.entries
                .sortedByDescending { it.value }
                .take(10)
                .map { BrandAffinity(it.key, it.value) },
            interestTags = normalizedTags.entries
                .sortedByDescending { it.value }
                .map { InterestTagScore(it.key, it.value) },
            lastUpdated = timeProvider.now()
        )

        cacheAndPersist(profile)
        return profile
    }

    suspend fun loadPersistedProfile(): UserProfile? {
        val entity = profileDao.getProfile() ?: return null
        return try {
            gson.fromJson(entity.profileJson, UserProfile::class.java).also {
                cachedProfile = it
                lastBuildTime = entity.lastUpdated
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun clearAll() {
        cachedProfile = null
        lastBuildTime = 0
        profileDao.deleteAll()
    }

    private suspend fun cacheAndPersist(profile: UserProfile) {
        cachedProfile = profile
        lastBuildTime = timeProvider.now()
        val json = gson.toJson(profile)
        profileDao.saveProfile(ProfileEntity(profileJson = json, lastUpdated = lastBuildTime))
    }

    private fun parseTags(tagsJson: String): List<String> {
        return try {
            gson.fromJson(tagsJson, Array<String>::class.java)?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
