plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.edmond.mevocarfinder"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.edmond.mevocarfinder"
        minSdk = 29
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
//    packagingOptions {
//        resources {
//            excludes += setOf(
//                // To compile the current version of UX Framework you need to add only these two lines:
//                "META-INF/DEPENDENCIES",
//                "META-INF/INDEX.LIST",
//            )
//        }
//    }

}

//configurations.all {
//    exclude(group = "com.google.guava", module = "listenablefuture")
//}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("com.mapbox.maps:android:11.1.0")
    implementation("com.google.code.gson:gson:2.9.1")
//    implementation("com.mapbox.navigationux:android:1.0.0-beta.20")

}

