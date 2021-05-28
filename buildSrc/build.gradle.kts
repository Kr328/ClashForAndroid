plugins {
    kotlin("jvm") version "1.5.10"
    `java-gradle-plugin`
}

apply("../gradle/extra.gradle.kts")

dependencies {
    implementation(rootProject.extra["androidPlugin"].toString())
}

gradlePlugin {
    plugins {
        create("golang") {
            id = "clash-build"
            implementationClass = "com.github.kr328.clash.tools.ClashBuildPlugin"
        }
    }
}
