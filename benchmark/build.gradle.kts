plugins {
    id("com.android.test")
}

android {
    namespace = "org.example.moxia.benchmark"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Mirror the app's macrobenchmark build type; a test module's variants follow these names.
    buildTypes {
        create("benchmark") {
            isDebuggable = false
            // A test APK needs a signature to install; the debug key is fine.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation("androidx.benchmark:benchmark-macro-junit4:1.5.0")
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
