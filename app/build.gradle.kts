plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.e68.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.e68.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true"
                )
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", "\"https://api.e68.ru/\"")
        }
        debug {
            buildConfigField("String", "BASE_URL", "\"https://dev-api.e68.ru/\"")
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
        viewBinding = true
    }

    // Добавляем packaging options для решения проблемы с iText
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/versions/**"
            excludes += "META-INF/native-image/**"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/*.properties"
            excludes += "META-INF/*.version"
            excludes += "META-INF/*.kotlin_module"
            excludes += "META-INF/services/org.slf4j.spi.SLF4JServiceProvider"

            // Для iText
            pickFirsts += "META-INF/native-image/reflect-config.json"
            pickFirsts += "META-INF/native-image/resource-config.json"
            pickFirsts += "META-INF/native-image/properties-config.json"
        }
    }
}

dependencies {
    // Kotlin
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Hilt
    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)
    implementation(libs.hilt.work)
    annotationProcessor(libs.hilt.work.compiler)

    // Room
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.room.ktx)

    // Navigation
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // WorkManager
    implementation(libs.work.runtime)
    implementation(libs.work.runtime.ktx)



// Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Glide
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // AppCompat (для XML layouts)
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Apache POI + iText - добавляем exclude для устранения конфликтов
    implementation(libs.poi) {
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
    }
    implementation(libs.poi.ooxml) {
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
    }
    implementation(libs.itext) {
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")

    }
    // Apache POI
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
// Log4j (обязательно для POI)
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")

    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.slf4j:slf4j-simple:1.7.36")

    // DataStore
    implementation(libs.datastore)

    // Security
    implementation(libs.security.crypto)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.common)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Yandex MapKit
    implementation("com.yandex.android:maps.mobile:4.6.1-full")

    // GPS (FusedLocationProvider)
    implementation("com.google.android.gms:play-services-location:21.2.0")

    implementation("com.itextpdf:itext7-core:7.2.5")
// Этот модуль содержит недостающий класс PdfEncodings
    implementation("com.itextpdf:io:7.2.5")
// И явно укажите kernel для надежности
    implementation("com.itextpdf:kernel:7.2.5")






}

