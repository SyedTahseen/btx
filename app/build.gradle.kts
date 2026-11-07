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
        // Firebase Web API key (public identifier, not a secret).
        buildConfigField("String", "FIREBASE_API_KEY", "\"AIzaSyBu7p1uD2vmMUZY4S5qGZjmtNGM2WE-M24\"")
        // Keep only English strings from libraries (smaller APK).
        resourceConfigurations += listOf("en")
    }

    // One fixed test key, so the SHA-1 registered in Firebase stays the same on every build (including CI).
    // This is a test key. Use your own private upload key for the Play Store.
    signingConfigs {
        create("betterx") {
            storeFile = file("betterx-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("betterx")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("betterx")
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
