plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.vicky.recsdk.tflite"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Don't compress TFLite model files
    androidResources {
        noCompress += listOf("tflite")
    }
}

dependencies {
    // Core SDK — only uses the public EmbeddingProvider interface
    implementation(project(":recsdk"))

    // MediaPipe Text Embedder (bundles TFLite runtime)
    implementation("com.google.mediapipe:tasks-text:0.10.14")

    // Test
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
