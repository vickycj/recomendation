package com.vicky.recsdk

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.vicky.recsdk.model.*
import com.vicky.recsdk.profile.ItemClassifier
import com.vicky.recsdk.profile.KeywordClassifier
import com.vicky.recsdk.profile.ProfileBuilder
import com.vicky.recsdk.ranking.BrandAffinityScorer
import com.vicky.recsdk.ranking.CategoryAffinityScorer
import com.vicky.recsdk.ranking.RecommendationRanker
import com.vicky.recsdk.ranking.TagAffinityScorer
import com.vicky.recsdk.storage.RecoDatabase
import com.vicky.recsdk.storage.entity.ItemCacheEntity
import com.vicky.recsdk.tracking.BehaviorScorer
import com.vicky.recsdk.tracking.EventTracker
import com.vicky.recsdk.util.SystemTimeProvider
import com.vicky.recsdk.util.TimeProvider
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArrayList

object RecoEngine {

    private const val TAG = "RecoEngine"

    @Volatile
    private var isInitialized = false
    private lateinit var config: RecoConfig
    private lateinit var database: RecoDatabase
    private lateinit var eventTracker: EventTracker
    private lateinit var profileBuilder: ProfileBuilder
    private lateinit var ranker: RecommendationRanker
    private lateinit var classifier: ItemClassifier
    private lateinit var timeProvider: TimeProvider
    private val gson = Gson()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val feedItems = CopyOnWriteArrayList<RecoItem>()

    /**
     * Initialize the SDK. Call once from Application.onCreate().
     *
     * @param context Application context
     * @param config SDK configuration
     * @param classifier Optional custom classifier (e.g., TFLite-based). Defaults to KeywordClassifier.
     */
    @JvmStatic
    @JvmOverloads
    fun init(
        context: Context,
        config: RecoConfig = RecoConfig(),
        classifier: ItemClassifier = KeywordClassifier()
    ) {
        if (isInitialized) {
            log("Already initialized, ignoring duplicate init call")
            return
        }

        this.config = config
        this.classifier = classifier
        this.timeProvider = SystemTimeProvider()
        this.database = RecoDatabase.getInstance(context)

        val behaviorScorer = BehaviorScorer(config.recencyHalfLifeDays, timeProvider)

        this.eventTracker = EventTracker(database.eventDao(), timeProvider)

        this.profileBuilder = ProfileBuilder(
            behaviorScorer = behaviorScorer,
            classifier = classifier,
            eventDao = database.eventDao(),
            itemCacheDao = database.itemCacheDao(),
            profileDao = database.profileDao(),
            timeProvider = timeProvider,
            enableTextClassification = config.enableTextClassification
        )

        this.ranker = RecommendationRanker(
            strategies = listOf(
                CategoryAffinityScorer(),
                BrandAffinityScorer(),
                TagAffinityScorer(classifier)
            )
        )

        isInitialized = true
        log("Initialized with config: $config")

        // Load persisted profile and prune old events
        scope.launch {
            profileBuilder.loadPersistedProfile()
            eventTracker.pruneOldEvents(config.eventRetentionDays)
        }
    }

    // For testing: allows injecting a custom TimeProvider
    internal fun initForTesting(
        database: RecoDatabase,
        config: RecoConfig = RecoConfig(),
        classifier: ItemClassifier = KeywordClassifier(),
        timeProvider: TimeProvider = SystemTimeProvider()
    ) {
        this.config = config
        this.classifier = classifier
        this.timeProvider = timeProvider
        this.database = database

        val behaviorScorer = BehaviorScorer(config.recencyHalfLifeDays, timeProvider)

        this.eventTracker = EventTracker(database.eventDao(), timeProvider)
        this.profileBuilder = ProfileBuilder(
            behaviorScorer = behaviorScorer,
            classifier = classifier,
            eventDao = database.eventDao(),
            itemCacheDao = database.itemCacheDao(),
            profileDao = database.profileDao(),
            timeProvider = timeProvider,
            enableTextClassification = config.enableTextClassification
        )
        this.ranker = RecommendationRanker(
            strategies = listOf(
                CategoryAffinityScorer(),
                BrandAffinityScorer(),
                TagAffinityScorer(classifier)
            )
        )
        isInitialized = true
    }

    /**
     * Feed items from the client's catalog.
     * The SDK does NOT call any API — the client feeds it data.
     * Call this whenever new product data is loaded.
     */
    @JvmStatic
    fun feedItems(items: List<RecoItem>) {
        checkInitialized()
        feedItems.clear()
        feedItems.addAll(items)
        log("Fed ${items.size} items")

        // Cache items to Room for profile building
        scope.launch {
            val entities = items.map { item ->
                ItemCacheEntity(
                    itemId = item.id,
                    title = item.title,
                    description = item.description,
                    category = item.category,
                    brand = item.brand,
                    price = item.price,
                    imageUrl = item.imageUrl,
                    tags = gson.toJson(item.tags),
                    metadata = gson.toJson(item.metadata),
                    lastUpdated = timeProvider.now()
                )
            }
            database.itemCacheDao().insertItems(entities)
        }
    }

    /**
     * Track a user interaction event.
     */
    @JvmStatic
    fun trackEvent(eventType: EventType, itemId: String) {
        trackEvent(RecoEvent(eventType, itemId, timeProvider.now()))
    }

    /**
     * Track a user interaction event with full RecoEvent.
     */
    @JvmStatic
    fun trackEvent(event: RecoEvent) {
        checkInitialized()
        log("Tracking ${event.eventType} on item ${event.itemId}")

        scope.launch {
            eventTracker.trackEvent(event)
            profileBuilder.invalidateCache()
        }
    }

    /**
     * Get ranked recommendations from the currently fed items.
     * This is a suspend function — call from a coroutine.
     */
    suspend fun getRecommendations(limit: Int = config.maxRecommendations): RecoResult {
        checkInitialized()
        if (feedItems.isEmpty()) {
            log("No items fed, returning empty recommendations")
            return RecoResult()
        }

        val profile = profileBuilder.getOrBuildProfile(config.profileRebuildIntervalMs)
        val result = ranker.rank(feedItems.toList(), profile, limit)
        log("Generated ${result.items.size} recommendations")
        return result
    }

    /**
     * Get recommendations with a callback (for non-coroutine callers).
     */
    @JvmStatic
    fun getRecommendationsAsync(
        limit: Int = 20,
        callback: (RecoResult) -> Unit
    ) {
        checkInitialized()
        scope.launch {
            val result = getRecommendations(limit)
            withContext(Dispatchers.Main) {
                callback(result)
            }
        }
    }

    /**
     * Get the current computed user profile.
     */
    suspend fun getUserProfile(): UserProfile {
        checkInitialized()
        return profileBuilder.getOrBuildProfile(config.profileRebuildIntervalMs)
    }

    /**
     * Get user profile with a callback.
     */
    @JvmStatic
    fun getUserProfileAsync(callback: (UserProfile) -> Unit) {
        checkInitialized()
        scope.launch {
            val profile = getUserProfile()
            withContext(Dispatchers.Main) {
                callback(profile)
            }
        }
    }

    /**
     * Clear all user data (for logout/GDPR compliance).
     */
    @JvmStatic
    fun clearUserData() {
        checkInitialized()
        log("Clearing all user data")
        scope.launch {
            eventTracker.clearAll()
            profileBuilder.clearAll()
            database.itemCacheDao().deleteAll()
        }
    }

    /**
     * Shutdown the engine. Call from testing or when the app is being destroyed.
     */
    @JvmStatic
    fun shutdown() {
        if (!isInitialized) return
        log("Shutting down")
        scope.cancel()
        RecoDatabase.destroyInstance()
        feedItems.clear()
        isInitialized = false
    }

    private fun checkInitialized() {
        check(isInitialized) {
            "RecoEngine is not initialized. Call RecoEngine.init(context) first."
        }
    }

    private fun log(message: String) {
        if (::config.isInitialized && config.enableDebugLogging) {
            Log.d(TAG, message)
        }
    }
}
