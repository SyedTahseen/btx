plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.betterx.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.betterx.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.2.0"
        // Optional: URL of YOUR backend that proxies an LLM (never ship an API key in the app).
        // Leave empty to use the built-in on-device coach.
        buildConfigField("String", "COACH_API_URL", "\"\"")
        // While true, "Start BetterX Plus" unlocks Plus locally so you can test it.
        // Set to "false" before publishing (and connect Play Billing).
        buildConfigField("boolean", "TEST_UNLOCK", "true")
    }

    androidResources {
        localeFilters += listOf("en")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signed with the debug key so CI builds install directly on a phone.
            // Create a real keystore for Play Store releases.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/*.version"
            excludes += "/kotlin-tooling-metadata.json"
            excludes += "/DebugProbesKt.bin"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
