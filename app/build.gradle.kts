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
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Clave de firma del APK que se descarga desde la web. Sus datos viven en ~/.gradle/gradle.properties
    // (INTU_KEYSTORE_*), nunca en el repositorio. Sin ellos, el release sale sin firmar.
    val releaseKeystore = providers.gradleProperty("INTU_KEYSTORE_FILE").orNull
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = providers.gradleProperty("INTU_KEYSTORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("INTU_KEY_ALIAS").get()
                keyPassword = providers.gradleProperty("INTU_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("release")
            // Solo celulares (ARM): las librerías x86 son para emuladores y casi duplican el tamaño del APK
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
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
    // Firestore solo se lee para copiar perfiles antiguos a Supabase
    implementation("com.google.firebase:firebase-firestore-ktx")
    // Fotos de perfil de pasajeros y conductores
    implementation("com.google.firebase:firebase-storage-ktx")
    // Avisos push del viaje (conductor aceptó, llegó, etc.) con la app minimizada
    implementation("com.google.firebase:firebase-messaging-ktx")
    // Cloud Functions: eliminar cuentas desde el panel de administración
    implementation("com.google.firebase:firebase-functions-ktx")
    // Google Sign-In para proveedor Google
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    // DataStore para preferencias
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    testImplementation(libs.junit)
    // org.json real para pruebas en la JVM (en Android lo trae el sistema)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
