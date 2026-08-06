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
