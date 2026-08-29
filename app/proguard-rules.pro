# JapanTripMap リリース用 R8/ProGuard ルール。

# ---- kotlinx.serialization ----
# @Serializable なクラス（TravelPlan など）の serializer がリフレクションで解決されるため、
# 難読化・削除されると実行時に SerializationException になる。公式推奨の keep ルール。
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# シリアライザ本体を保持。
-keepclassmembers class **$$serializer { *; }
-keep,includedescriptorclasses class com.example.japantripmap.**$$serializer { *; }
-keepclassmembers class com.example.japantripmap.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.japantripmap.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable アノテーションが付いたクラスとそのメンバを保持。
-keep @kotlinx.serialization.Serializable class com.example.japantripmap.** { *; }

# ---- Play In-App Review / Play Services ----
# review-ktx が参照する Play Services 内部アノテーション。実行時には不要なので警告のみ抑制。
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite

# ---- AdMob (GMA Next-Gen SDK) + WorkManager / androidx.startup ----
# Next-Gen SDK は内部で WorkManager を使い、androidx.startup.InitializationProvider から
# WorkManagerInitializer 経由で初期化される。R8 の圧縮で Room の WorkDatabase 生成に必要な
# クラス／メンバが削られると、起動時に「Failed to create an instance of WorkDatabase」で落ちる。
# App Startup と WorkManager 一式を保持して初期化経路を守る。
-keep class androidx.startup.** { *; }
-keep class * implements androidx.startup.Initializer { *; }
-keep class androidx.work.** { *; }
-keep class androidx.work.impl.** { *; }
# WorkManager が参照する Room / SQLite まわり。
-keep class androidx.room.** { *; }
-keep class androidx.sqlite.** { *; }
# AdMob Next-Gen SDK 本体（リフレクション参照・広告表示に必要なクラスを削らない）。
-keep class com.google.android.libraries.ads.mobile.sdk.** { *; }
-dontwarn com.google.android.libraries.ads.mobile.sdk.**
# UMP（GDPR 同意フォーム）。同意フォームを動的に表示するため保持する。
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.ump.**
