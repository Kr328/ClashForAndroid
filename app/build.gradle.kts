import java.util.*

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

android {
    compileSdk = buildTargetSdkVersion

    flavorDimensions(buildFlavor)

    defaultConfig {
        applicationId = "com.github.kr328.clash"

        minSdk = buildMinSdkVersion
        targetSdk = buildTargetSdkVersion

        versionCode = buildVersionCode
        versionName = buildVersionName

        resConfigs("zh-rCN", "zh-rHK", "zh-rTW")

        resValue("string", "release_name", "v$buildVersionName")
        resValue("integer", "release_code", "$buildVersionCode")
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    productFlavors {
        create("foss") {
            dimension = "foss"
            versionNameSuffix = ".foss"
            applicationIdSuffix = ".foss"
        }
        create("premium") {
            dimension = "premium"
            versionNameSuffix = ".premium"

            if (buildFlavor == "premium") {
                val localFile = rootProject.file("local.properties")
                if (localFile.exists()) {
                    val appCenterKey = localFile.inputStream()
                        .use { Properties().apply { load(it) } }
                        .getProperty("appcenter.key", null)

                    if (appCenterKey != null) {
                        buildConfigField("String", "APP_CENTER_KEY", "\"$appCenterKey\"")
                    } else {
                        buildConfigField("String", "APP_CENTER_KEY", "null")
                    }
                } else {
                    buildConfigField("String", "APP_CENTER_KEY", "null")
                }
            }
        }
    }

    val signingFile = rootProject.file("keystore.properties")
    if (signingFile.exists()) {
        val properties = Properties().apply {
            signingFile.inputStream().use {
                load(it)
            }
        }
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(properties.getProperty("storeFile")!!)
                storePassword = properties.getProperty("storePassword")!!
                keyAlias = properties.getProperty("keyAlias")!!
                keyPassword = properties.getProperty("keyPassword")!!
            }
        }
        buildTypes {
            named("release") {
                signingConfig = signingConfigs["release"]
            }
        }
    }

    buildFeatures {
        dataBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    splits {
        abi {
            isEnable = true
            isUniversalApk = true
        }
    }
}

dependencies {
    val premiumImplementation by configurations

    api(project(":core"))
    api(project(":service"))
    api(project(":design"))
    api(project(":common"))

    premiumImplementation("com.microsoft.appcenter:appcenter-analytics:$appcenterVersion")
    premiumImplementation("com.microsoft.appcenter:appcenter-crashes:$appcenterVersion")

    implementation(kotlin("stdlib-jdk7"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion")
    implementation("androidx.core:core-ktx:$coreVersion")
    implementation("androidx.activity:activity:$activityVersion")
    implementation("androidx.appcompat:appcompat:$appcompatVersion")
    implementation("androidx.coordinatorlayout:coordinatorlayout:$coordinatorlayoutVersion")
    implementation("androidx.recyclerview:recyclerview:$recyclerviewVersion")
    implementation("androidx.fragment:fragment:$fragmentVersion")
    implementation("com.google.android.material:material:$materialVersion")
}

task("cleanRelease", type = Delete::class) {
    delete(file("release"))
}

afterEvaluate {
    tasks["clean"].dependsOn(tasks["cleanRelease"])
}