-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

-keep class com.gestorfacil.app.data.model.** { *; }

-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class androidx.compose.** { *; }

-dontwarn kotlinx.**
-dontwarn javax.**
