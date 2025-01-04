plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.hackverse"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.hackverse"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.1"

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
        buildConfig = true
    }

}

dependencies {
    implementation(libs.androidx.constraintlayout.v214)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(platform(libs.firebase.bom))
    implementation (libs.cloudinary.android)
    implementation (libs.glide)
    implementation(libs.core)
    implementation(libs.androidx.ui.text.android)
    annotationProcessor (libs.compiler)
    implementation (libs.compact.calendar.view)
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-dynamic-links:21.1.0")
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.photoview)
    implementation ("com.google.android.gms:play-services-auth:21.3.0")
    implementation(libs.androidx.core.splashscreen)
    implementation (libs.material.v190)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}