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
    kapt("androidx.room:room-compiler:$roomVersion")

    api(project(":core"))
    api(project(":common"))

    implementation(project(":kaidl:kaidl-runtime"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.core:core-ktx:$coreVersion")
    implementation("dev.rikka.rikkax.preference:multiprocess:$muiltprocessVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
}

afterEvaluate {
    android {
        libraryVariants.forEach {
            sourceSets[it.name].java.srcDir(buildDir.resolve("generated/ksp/${it.name}/kotlin"))
        }
    }
}