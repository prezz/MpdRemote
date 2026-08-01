plugins {
    // AGP 9 provides built-in Kotlin support, so no separate Kotlin plugin is applied.
    alias(libs.plugins.android.application)
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "net.prezz.mpr"
    compileSdk = 37

    defaultConfig {
        applicationId = "net.prezz.mpr2"
        minSdk = 36
        targetSdk = 37
        versionCode = 200
        versionName = "2.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-project.txt",
            )
            // Uses debug signing for now; a real release keystore is a later task.
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.viewpager2)
    implementation(libs.commons.csv)
    implementation(libs.kotlinx.coroutines.android)
}
