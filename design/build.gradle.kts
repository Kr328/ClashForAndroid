plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
}

android {
    compileSdk = buildTargetSdkVersion

    flavorDimensions(buildFlavor)

    defaultConfig {
        minSdk = buildMinSdkVersion
        targetSdk = buildTargetSdkVersion

        versionCode = buildVersionCode
        versionName = buildVersionName

        consumerProguardFiles("consumer-rules.pro")
    }

    productFlavors {
        create("foss") {
            dimension = "foss"
        }
        create("premium") {
            dimension = "premium"
        }
    }

    buildFeatures {
        dataBinding = true
    }
}

dependencies {
    api(project(":common"))
    api(project(":core"))
    api(project(":service"))

    implementation(Libs.coroutines)
    implementation(Libs.core)
    implementation(Libs.appCompat)
    implementation(Libs.activity)
    implementation(Libs.fragment)
    implementation(Libs.viewPager2)
    implementation(Libs.recyclerView)
    implementation(Libs.material)
    implementation(Libs.coordinatorLayout)
    implementation(Libs.serializationJson)
}
