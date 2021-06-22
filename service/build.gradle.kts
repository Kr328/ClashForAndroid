plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("kotlinx-serialization")
    id("com.google.devtools.ksp")
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
}

dependencies {
    ksp(project(":kaidl:kaidl"))
    kapt(Libs.roomApt)

    api(project(":core"))
    api(project(":common"))

    implementation(project(":kaidl:kaidl-runtime"))

    implementation(Libs.coroutines)
    implementation(Libs.core)
    Libs.room.forEach(::implementation)
    implementation(Libs.multiProcess)
    implementation(Libs.serializationJson)
}

afterEvaluate {
    android {
        libraryVariants.forEach {
            sourceSets[it.name].java.srcDir(buildDir.resolve("generated/ksp/${it.name}/kotlin"))
        }
    }
}