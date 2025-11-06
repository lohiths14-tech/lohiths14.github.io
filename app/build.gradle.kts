plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.smartfind.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smartfind.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room schema export directory
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.expandProjection", "true")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Split by ABI to reduce APK size
            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            }
        }
        debug {
            isMinifyEnabled = false
            // applicationIdSuffix = ".debug" // Removed to match google-services.json package name
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
        viewBinding = true
        buildConfig = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:1.9.25")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.25")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.25")
        force("org.jetbrains.kotlin:kotlin-stdlib-common:1.9.25")
    }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.25")

    // Core Android
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    // Material Design 3
    implementation("com.google.android.material:material:1.13.0")

    // CameraX
    implementation("androidx.camera:camera-camera2:1.5.0")
    implementation("androidx.camera:camera-view:1.5.0")
    implementation("androidx.camera:camera-lifecycle:1.5.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // TensorFlow Lite - using consistent versions to avoid conflicts
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0") // GPU acceleration
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4") {
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
        exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
    }
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4") {
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
        exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
    }

    // Location Services
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // JSON Serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Image Loading - Glide
    implementation("com.github.bumptech.glide:glide:5.0.5")
    ksp("com.github.bumptech.glide:compiler:5.0.5")

    // Lottie Animations
    implementation("com.airbnb.android:lottie:6.6.9")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48.1")
    ksp("com.google.dagger:hilt-compiler:2.48.1")
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")

    // Firebase for production monitoring
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")

    // LeakCanary for memory leak detection (debug only)
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.25")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // NEW FEATURES - v2.0
    // Onboarding
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.tbuonomo:dotsindicator:5.0")

    // In-app review
    implementation("com.google.android.play:review-ktx:2.0.1")

    // Charts for statistics (optional)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}
