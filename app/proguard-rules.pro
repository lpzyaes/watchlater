-dontobfuscate
-ignorewarnings

# Architecture and data models
-keep class com.lambdasoup.watchlater.data.** { *; }
-keep class com.lambdasoup.watchlater.viewmodel.** { *; }
-keep class com.lambdasoup.tea.** { *; }

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Moshi & Moshi-Kotlin
-keepclassmembers class * {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }
-dontwarn kotlin.reflect.**
-keep class kotlin.reflect.jvm.internal.** { *; }

# Koin
-dontwarn org.koin.**
-keep class org.koin.** { *; }

# Glide
-dontwarn com.bumptech.glide.**

# AndroidX & Google Material
-dontwarn androidx.**
-dontwarn com.google.android.material.**

# Test & JUnit dependencies bundled in library modules
-dontwarn org.junit.**
-dontwarn junit.**
-dontwarn org.hamcrest.**
-dontwarn androidx.test.**
-dontwarn java.lang.management.**
-dontwarn javax.management.**

