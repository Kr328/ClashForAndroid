import java.io.FileOutputStream
import java.net.URL
import java.time.Duration

plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlinx-serialization")
    id("clash-build")
}

val geoipDatabaseUrl =
    "https://github.com/Dreamacro/maxmind-geoip/releases/latest/download/Country.mmdb"
val geoipInvalidate = Duration.ofDays(7)!!
val geoipOutput = buildDir.resolve("intermediates/golang_blob")

android {
    compileSdk = buildTargetSdkVersion

    ndkVersion = buildNdkVersion

    flavorDimensions(buildFlavor)

    defaultConfig {
        minSdk = buildMinSdkVersion
        targetSdk = buildTargetSdkVersion

        versionCode = buildVersionCode
        versionName = buildVersionName

        consumerProguardFiles("consumer-rules.pro")

        externalNativeBuild {
            cmake {
                abiFilters("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
                arguments(
                    "-DGO_SOURCE:STRING=$golangSource",
                    "-DGO_OUTPUT:STRING=$golangBuild",
                    "-DFLAVOR_NAME=$buildFlavor",
                )
            }
        }
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

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}

dependencies {
    api(project(":common"))

    implementation(kotlin("stdlib-jdk7"))
    implementation("androidx.core:core-ktx:$coreVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
}

repositories {
    mavenCentral()
}

task("downloadGeoipDatabase") {
    val databaseFile = geoipOutput.resolve("Country.mmdb")
    val moduleFile = geoipOutput.resolve("go.mod")
    val sourceFile = geoipOutput.resolve("blob.go")

    val moduleContent = """
        module "cfa/blob"
    """.trimIndent()

    val sourceContent = """
        package blob
        
        import _ "embed"
        
        //go:embed Country.mmdb
        var GeoipDatabase []byte
    """.trimIndent()

    onlyIf {
        System.currentTimeMillis() - databaseFile.lastModified() > geoipInvalidate.toMillis()
    }

    doLast {
        geoipOutput.mkdirs()

        moduleFile.writeText(moduleContent)
        sourceFile.writeText(sourceContent)

        URL(geoipDatabaseUrl).openConnection().getInputStream().use { input ->
            FileOutputStream(databaseFile).use { output ->
                input.copyTo(output)
            }
        }
    }
}

afterEvaluate {
    val downloadTask = tasks["downloadGeoipDatabase"]

    tasks.forEach {
        if (it.name.startsWith("externalGolangBuild")) {
            it.dependsOn(downloadTask)
        }
    }
}