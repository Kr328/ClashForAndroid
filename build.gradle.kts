@file:Suppress("UNUSED_VARIABLE")

import java.net.URL

buildscript {
    apply(from = "gradle/extra.gradle.kts")
    repositories {
        google()
        gradlePluginPortal()
    }
    dependencies {
        classpath(rootProject.extra["androidPlugin"].toString())
        classpath(rootProject.extra["kotlinPlugin"].toString())
        classpath(rootProject.extra["serializationPlugin"].toString())
        classpath(rootProject.extra["kspPlugin"].toString())
    }
}

allprojects {
    apply(from = "${rootDir.path}/gradle/extra.gradle.kts")
}

task("clean", type = Delete::class) {
    delete(rootProject.buildDir)
}

tasks.named<Wrapper>("wrapper") {
    distributionType = Wrapper.DistributionType.ALL

    doLast {
        val sha256 = URL("$distributionUrl.sha256").openStream()
            .use { it.reader().readText().trim() }

        file("gradle/wrapper/gradle-wrapper.properties")
            .appendText("distributionSha256Sum=$sha256")
    }
}