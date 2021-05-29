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
}

dependencies {
    compileOnly(project(":hideapi"))

    implementation(Libs.coroutines)
    implementation(Libs.core)
}
