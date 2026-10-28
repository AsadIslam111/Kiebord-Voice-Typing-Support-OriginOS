# ProGuard / R8 rules for Kiebord

# Keep JNI / Rust NativeEngine methods
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class io.kiebord.feature.ime.NativeEngine {
    *;
}

# Keep Activities, Services, and components launched by intent or reflection
-keep class io.kiebord.app.MainActivity { *; }
-keep class io.kiebord.feature.ime.KiebordService { *; }
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
-keep class io.kiebord.common.model.** { *; }
-keep class io.kiebord.theme.** { *; }
-keep class io.kiebord.data.layout.** { *; }

