plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "no.hiof.mobilproggroup3"
    compileSdk = 34

    defaultConfig {
        applicationId = "no.hiof.mobilproggroup3"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }
    //Its not the latest versions, but its a stable version which is good enough
    //i cannot stress this enough DO NOT FUCK AROUND WITH THIS
    //versions 1.5.3 and below as well as 1.5.5 and above are cursed
    //You can check compatibility here
    //https://developer.android.com/jetpack/androidx/releases/compose-kotlin
    //we will be using compiler version 1.5.4 with kotlin version 1.9.20

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
//Same as compiler version above, do not mess with these dependencies even if some are redundant
//if you need to add specific dependencies for various functionalities use https://mvnrepository.com
//do not pick up dependencies from stackoverflow or reddit which are most likely deprecated
//and will fuck up our currently working stuff
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    //the dependencies for camerax
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    //dependencies ML Kit Text Recognition
    implementation(libs.google.mlkit.text.recognition)
    implementation(libs.vision.common)

    // https://mvnrepository.com/artifact/androidx.navigation/navigation-fragment-compose
    implementation(libs.androidx.navigation.fragment.compose)

    // https://mvnrepository.com/artifact/androidx.navigation/navigation-compose
    runtimeOnly(libs.androidx.navigation.compose)

    implementation (libs.navigation.compose.v260)
    implementation (libs.androidx.activity.compose.v170)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
