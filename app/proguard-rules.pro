# Maddookie R8 / ProGuard Obfuscation & Security Rules
-repackageclasses 'o'
-allowaccessmodification

# Strip all debug/verbose/info/warning/error log calls in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Preserve kotlinx.serialization & Room entities
-keepattributes *Annotation*
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn okhttp3.**
-dontwarn okio.**
