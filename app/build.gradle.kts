import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.diary.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.diary.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 26455
        versionName = "2.64.55-experimental"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "GITHUB_OWNER", "\"kUIsii\"")
        buildConfigField("String", "GITHUB_REPO", "\"DiaryApp\"")
        buildConfigField("String", "GITHUB_TOKEN", "\"${localProps.getProperty("GITHUB_TOKEN", "")}\"")
        buildConfigField("String", "AMAP_API_KEY", "\"${localProps.getProperty("AMAP_API_KEY", "")}\"")

        manifestPlaceholders["AMAP_API_KEY"] = localProps.getProperty("AMAP_API_KEY", "")
    }

    signingConfigs {
        getByName("debug") {
            // 使用默认 debug keystore 签名 release
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
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
    flavorDimensions += "version"
    productFlavors {
        create("stable") {
            dimension = "version"
            applicationId = "com.diary.app"
            versionName = "2.63.08"
        }
        create("experimental") {
            dimension = "version"
            applicationId = "com.diary.app.experimental"
            versionCode = 26466
            versionName = "2.64.66-experimental"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // WorkManager for periodic auto-backup
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Gson for JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // WebKit (WebViewAssetLoader)
    implementation("androidx.webkit:webkit:1.8.0")

    // Biometric
    implementation("androidx.biometric:biometric:1.1.0")

    // Health Connect API
    implementation("androidx.health.connect:connect-client:1.1.0-alpha10")

    // Amap (高德地图) SDK
    implementation("com.amap.api:3dmap:10.0.600")

    testImplementation("junit:junit:4.13.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
