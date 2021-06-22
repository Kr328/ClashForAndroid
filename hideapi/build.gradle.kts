plugins {
    id("com.android.library")
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
