plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    compileSdk = buildTargetSdkVersion

    defaultConfig {
        minSdk = buildMinSdkVersion
        targetSdk = buildTargetSdkVersion

        versionCode = buildVersionCode
        versionName = buildVersionName

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    compileOnly(project(":hideapi"))

    implementation("androidx.core:core-ktx:$coreVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion")
}

repositories {
    mavenCentral()
    google()
}
