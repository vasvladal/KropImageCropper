// ========== app/build.gradle.kts ==========
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.kropimagecropper"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.kropimagecropper"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Android Core
    implementation(libs.android.core.ktx)
    implementation(libs.android.lifecycle.runtime.ktx)
    implementation(libs.android.activity.compose)

    implementation(libs.androidx.material3)

// Use latest version

    // Compose BOM and UI
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)

    // 🎯 KROP LIBRARY - Using version catalog!
    implementation(libs.bundles.krop)
    // OR individually:
    // implementation(libs.krop.ui)
    // implementation(libs.krop.extensions.filekit)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Permissions
    implementation(libs.accompanist.permissions)

    // Image loading (Optional)
    implementation(libs.coil.compose)
    //implementation(libs.androidx.navigation.fragment.ktx)
    //implementation(libs.androidx.navigation.ui.ktx)

    // Testing
    testImplementation(libs.bundles.testing)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.android.testing)
    debugImplementation(libs.bundles.debug.testing)

    implementation(libs.reorderable)

    implementation(libs.androidx.navigation.compose)
}