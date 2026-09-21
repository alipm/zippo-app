plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // 🟢 FIXED: Resolves via matching version catalog definitions
    alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "com.zippo.app"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        applicationId = "com.zippo.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {

        debug {
            buildConfigField(
                "String",
                "BASE_URL",
                "\"http://10.0.2.2:3000/\""
//                "\"https://zippo-backend.onrender.com/\""
            )
        }
        release {
            buildConfigField(
                "String",
                "BASE_URL",
                "\"https://zippo-backend.onrender.com/\""
            )
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
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // 1. FORCE RIGID CONSTRAINTS TO BLOCK WRONG ALPHA VERSIONS INTERNALLY
    constraints {
        implementation("androidx.core:core:1.13.1") {
            because("Version 1.17.0 requires unreleased Android API 36 platform structures")
        }
        implementation("androidx.core:core-ktx:1.13.1") {
            because("Version 1.17.0 requires unreleased Android API 36 platform structures")
        }
        implementation("androidx.activity:activity:1.9.3") {
            because("Version 1.12.4 breaks compileSdk 35 toolchains")
        }
        implementation("androidx.activity:activity-compose:1.9.3") {
            because("Version 1.12.4 breaks compileSdk 35 toolchains")
        }
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4") {
            because("Prevents automatic pulling of higher unstable internal API targets")
        }
    }
    // 2. EXPLICIT STABLE COMPATIBLE VERSIONS FOR API 35 (Replaces the broken libs.androidx.X lines)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    // 3. CORE JETPACK COMPOSE UI CORE FRAMEWORKS
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    // 4. RETROFIT & HTTP PIPELINE NETWORK TRANSPORTS
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // GOOGLE HIGH-ACCURACY PLAY SERVICES LOCATION PROVIDER
    implementation("com.google.android.gms:play-services-location:21.3.0")
    // REAL-TIME WEBSOCKET DUPLEX COMMUNICATION SOCKET.IO
    implementation("io.socket:socket.io-client:2.1.0") {
        exclude(group = "org.json", module = "json") // Prevents duplication conflicts with native Android JSON tools
    }
    // NATIVE PERSISTENT ROOM SQLITE COMPONENT LAYOUTS
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // OPEN-SOURCE HIGHER ACCURACY DISK MAP CANVAS OVERLAYS
    implementation("org.osmdroid:osmdroid-android:6.1.18")
}
