package com.vicky.recsdk.demo

import android.app.Application
import com.vicky.recsdk.RecoConfig
import com.vicky.recsdk.RecoEngine
import com.vicky.recsdk.tflite.TfLiteEmbeddingProvider

class DemoApplication : Application() {

    private var embeddingProvider: TfLiteEmbeddingProvider? = null

    override fun onCreate() {
        super.onCreate()

        // Create TFLite embedding provider for semantic similarity
        embeddingProvider = TfLiteEmbeddingProvider(this)

        RecoEngine.init(
            context = this,
            config = RecoConfig(
                maxRecommendations = 20,
                recencyHalfLifeDays = 7,
                enableTextClassification = true,
                enableDebugLogging = true,
                embeddingProvider = embeddingProvider
            )
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        embeddingProvider?.close()
    }
}
