plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.nebada"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.nebada"
        minSdk = 24
        targetSdk = 35
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
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")
    // SwipeRefreshLayout 추가
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    // JSON 처리 및 네트워킹
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    // OkHttp 로깅 인터셉터 추가 (디버깅용)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}