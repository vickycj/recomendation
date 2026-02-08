package com.vicky.recsdk.demo

import android.app.Application
import com.vicky.recsdk.RecoConfig
import com.vicky.recsdk.RecoEngine

class DemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        RecoEngine.init(
            context = this,
            config = RecoConfig(
                maxRecommendations = 20,
                recencyHalfLifeDays = 7,
                enableTextClassification = true,
                enableDebugLogging = true
            )
        )
    }
}
