# Keep public SDK API
-keep class com.vicky.recsdk.RecoEngine { *; }
-keep class com.vicky.recsdk.RecoConfig { *; }
-keep class com.vicky.recsdk.model.** { *; }
-keep class com.vicky.recsdk.profile.ItemClassifier { *; }

# Room
-keep class com.vicky.recsdk.storage.** { *; }
