plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

configurations.all {
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib-common:1.9.0")
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.0")
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.0")
}

android {
    namespace = "com.sqq.pdfget"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sqq.pdfget"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.datastore.core.android)
    implementation(libs.androidx.datastore.preferences.core.android)
    implementation(libs.firebase.crashlytics.buildtools)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // WebView integration for Compose
    implementation(libs.androidx.webkit)
    implementation(libs.accompanist.webview)

    // Permission handling
    implementation(libs.accompanist.permissions)

    // Document picker
    implementation(libs.androidx.documentfile.documentfile)
    implementation(libs.androidx.compose.foundation.foundation)

    implementation(libs.androidx.material.icons.extended)
    implementation(libs.material3)

}