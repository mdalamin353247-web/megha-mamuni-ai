# ============================================================
# মেঘা মামুনি - ProGuard Rules
# Optimized for 2GB RAM Android 11+ devices
# ============================================================

# Keep app classes
-keep class com.meghamamuni.** { *; }
-keep class com.megha.mamuni.** { *; }

# Keep data classes (Room, JSON parsing)
-keep class com.meghamamuni.assistant.ChatRepository$* { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers @androidx.room.Entity class * { *; }

# Retrofit + OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Gson
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** { *; }

# Lottie
-keep class com.airbnb.lottie.** { *; }

# BiometricPrompt
-keep class androidx.biometric.** { *; }

# TextToSpeech & SpeechRecognizer
-keep class android.speech.** { *; }
-keep class android.speech.tts.** { *; }

# EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }

# Remove logging in release (save RAM + battery)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
}

# General optimizations
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively
-overloadaggressively
