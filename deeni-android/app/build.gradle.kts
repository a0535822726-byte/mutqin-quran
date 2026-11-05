plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

android {
    namespace = "com.deeni.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.deeni.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 5
        versionName = "1.0.4"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    testOptions { unitTests.isReturnDefaultValues = true }
}
