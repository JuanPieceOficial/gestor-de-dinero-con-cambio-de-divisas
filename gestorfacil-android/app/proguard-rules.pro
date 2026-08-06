-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

-keep class com.gestorfacil.app.data.model.** { *; }

# Wallet integrada: BouncyCastle
-keep class org.bouncycastle.crypto.** { *; }
-keep class org.bouncycastle.asn1.** { *; }
-dontwarn org.bouncycastle.**

# Wallet integrada: ZXing
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class androidx.compose.** { *; }

-dontwarn kotlinx.**
-dontwarn javax.**
