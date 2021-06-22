import java.nio.charset.StandardCharsets

rootProject.extra.run {
    val kotlinVersion = "1.5.10"
    set("androidPlugin", "com.android.tools.build:gradle:4.2.1")
    set("kotlinPlugin", "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    set("serializationPlugin", "org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
    set("kspPlugin", "com.google.devtools.ksp:symbol-processing-gradle-plugin:1.5.10-1.0.0-beta01")
}

repositories {
    google()
    mavenCentral()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = StandardCharsets.UTF_8.toString()
}
