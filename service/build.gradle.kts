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

    buildTypes {
        named("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    productFlavors {
        create("foss") {
            dimension = "foss"
        }
        create("premium") {
            dimension = "premium"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    sourceSets {
        named("debug") {
            java.srcDir(buildDir.resolve("generated/ksp/debug/kotlin"))
        }
        named("release") {
            java.srcDir(buildDir.resolve("generated/ksp/release/kotlin"))
        }
    }
}

dependencies {
    ksp(project(":kaidl:kaidl"))
    kapt("androidx.room:room-compiler:$roomVersion")

    api(project(":core"))
    api(project(":common"))

    implementation(kotlin("stdlib-jdk7"))
    implementation(project(":kaidl:kaidl-runtime"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.core:core-ktx:$coreVersion")
    implementation("dev.rikka.rikkax.preference:multiprocess:$muiltprocessVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
}