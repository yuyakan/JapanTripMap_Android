import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // AGP 9.x は Kotlin サポートを built-in で自動適用するため、
    // kotlin.android プラグインは明示的に apply しない。
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// リリース署名の設定を keystore.properties から読み込む（Git 管理外）。
// storeFile / storePassword / keyAlias / keyPassword を定義する。ファイルが無ければ署名設定はスキップ。
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

// Google Maps の API キーは local.properties（Git 管理外）から読み込み、
// manifestPlaceholders 経由で AndroidManifest に埋め込む。
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val mapsApiKey: String = localProps.getProperty("MAPS_API_KEY", "")

android {
    namespace = "com.example.japantripmap"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.kanbe1365.japantripmap"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AndroidManifest の @string/google_maps_key の代わりに使うプレースホルダ。
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // keystore.properties がある場合のみリリース署名を適用する。
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro",
                )
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // AGP 9.x の built-in Kotlin では kotlinOptions ではなく kotlin { } を使う。
    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }
    buildFeatures {
        compose = true
        // AppReviewManager でデバッグ判定（BuildConfig.DEBUG）に使う。
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.maps.compose)
    // アプリ内レビュー（Play In-App Review）。ktx で suspend 拡張が使える。
    implementation(libs.play.review)
    implementation(libs.play.review.ktx)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    // ユニットテストで Prefecture（Offset を参照）をロードするために必要。
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.graphics)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
