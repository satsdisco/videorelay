# Keep nostr event classes for serialization
-keep class com.videorelay.app.data.nostr.** { *; }
-keep class com.videorelay.app.domain.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# secp256k1
-keep class fr.acinq.secp256k1.** { *; }
