plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.cyebrcina.pos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cyebrcina.pos"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Fire Hut's real device/EPOS backend (AAtish/Admin). See openapi.yaml at repo root.
        buildConfigField("String", "DEVICE_API_BASE_URL", "\"https://admin.firehut.uk\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.window.size)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Fire Hut device/EPOS API client (see data/remote/, openapi.yaml)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp.core)
    debugImplementation(libs.okhttp.logging.interceptor)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.socketio.client)
    implementation(libs.coil.compose)

    // Imin's built-in printer SDK (PrinterHelper / NeoPrinterManager, AIDL-based). Verified
    // directly against https://github.com/iminsoftware/IminPrinterLibrary (their own source,
    // not their gated docs site) — see PRINTER_SETUP.md. Check their tags for a newer version
    // before shipping: https://github.com/iminsoftware/IminPrinterLibrary/tags
    implementation("com.github.iminsoftware:IminPrinterLibrary:V2.0.0.19")
}
