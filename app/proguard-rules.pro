# ProGuard / R8 rules for Kiebord

# Keep JNI / Rust NativeEngine methods
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.codepotro.borno.feature.ime.NativeEngine {
    *;
}

# Keep Activities, Services, and components launched by intent or reflection
-keep class com.codepotro.borno.kiebord.MainActivity { *; }
-keep class com.codepotro.borno.feature.ime.KiebordService { *; }
-keep class com.codepotro.borno.feature.ime.voice.** { *; }
-keep class androidx.emoji2.emojipicker.BundledEmojiListLoader { *; }

# Keep Kotlinx Serialization data models
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt

-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.codepotro.borno.common.model.** { *; }
-keep class com.codepotro.borno.theme.** { *; }
-keep class com.codepotro.borno.data.layout.** { *; }

