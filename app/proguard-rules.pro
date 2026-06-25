# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.diary.app.data.DiaryEntry { *; }
-keep class com.diary.app.data.TrashEntry { *; }
-keep class com.diary.app.data.TodoItem { *; }
-keep class com.diary.app.data.** { *; }
-keep class com.diary.app.ai.** { *; }
-keepclassmembers class * extends com.google.gson.TypeAdapter
-keepclassmembers class * implements com.google.gson.TypeAdapterFactory

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# WebView JS interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Compose
-dontwarn androidx.compose.**

# Amap
-keep class com.amap.api.** { *; }
-keep class com.autonavi.** { *; }
-dontwarn com.amap.api.**
-dontwarn com.autonavi.**

# Health Connect
-keep class androidx.health.connect.** { *; }
