plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.intu.taxi"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.intu.taxi"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    defaultConfig {
        // Access token de Mapbox en recursos para uso en runtime
        // Lee de local.properties (para builds locales/APK) o variables de entorno (para CI)
        val mapboxToken = providers.gradleProperty("MAPBOX_ACCESS_TOKEN").orNull
            ?: System.getenv("MAPBOX_ACCESS_TOKEN")
            ?: ""
        resValue("string", "mapbox_access_token", mapboxToken)
        buildConfigField("String", "SUPABASE_URL", "\"https://vkguzpciwpfvaeyedepl.supabase.co\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZrZ3V6cGNpd3BmdmFleWVkZXBsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjMwNzYxMjYsImV4cCI6MjA3ODY1MjEyNn0.gHosYEPeqBHMjkezz5b9wuMQ6-PRFONcYrUuO62TYBc\"")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Navigation & icons
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    // Mapbox Maps SDK
    implementation(libs.mapbox.maps.android)
    
    // OkHttp para sugerencias de geocodificación
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Coil para cargar imágenes en Compose desde internet
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Firebase BOM y módulos necesarios
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    // Google Sign-In para proveedor Google
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    // DataStore para preferencias
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
