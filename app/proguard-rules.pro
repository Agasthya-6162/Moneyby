# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# SQLCipher rules for net.zetetic package
-keep class net.zetetic.** { *; }
-dontwarn net.zetetic.**

# Moshi Codegen rules
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**
-keep class * {
    @com.squareup.moshi.JsonClass public *;
}
-keep class * extends com.squareup.moshi.JsonAdapter
-keep class * {
    @com.squareup.moshi.Json public *;
}

# Room rules (usually handled by AAR but added for safety)
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.room.RoomDatabase {
    <init>(...);
}
-dontwarn androidx.room.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-keep class kotlinx.coroutines.android.** { *; }

# Keeping all models that might be serialized
-keep class com.example.moneyby.data.** { *; }

# BCrypt
-keep class at.favre.lib.crypto.bcrypt.** { *; }