plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.synapse.gaming"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.synapse.gaming"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "0.8"
        ndk { abiFilters += "arm64-v8a" }
    }
    buildFeatures { compose = true }
    kotlinOptions { jvmTarget = "17" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("com.github.Swordfish90:LibretroDroid:0.12.0")
}
