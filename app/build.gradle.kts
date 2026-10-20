plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The PAX NeptuneLite SDK is proprietary and is not committed (see app/libs/README.md).
// When the jar is present, build the real PAX printer; otherwise a no-printer stub.
val paxSdk = fileTree("libs") { include("NeptuneLiteApi*.jar") }
val hasPaxSdk = !paxSdk.isEmpty

android {
    namespace = "com.haustoys.pos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.haustoys.pos"
        minSdk = 22
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signed with the debug key so the APK installs directly; use your own key for production.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    sourceSets["main"].java.srcDir(if (hasPaxSdk) "src/pax/java" else "src/nopax/java")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    if (hasPaxSdk) implementation(paxSdk)

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    testImplementation("junit:junit:4.13.2")
}
