plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.local.barkfwd"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.local.barkfwd"
        minSdk = 26
        targetSdk = 34
        versionCode = 4
        versionName = "1.3"
    }
    signingConfigs {
        create("fixed") {
            storeFile = file("keystore.jks")
            storePassword = "barkfwd12"
            keyAlias = "barkfwd"
            keyPassword = "barkfwd12"
        }
    }
    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("fixed")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fixed")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
