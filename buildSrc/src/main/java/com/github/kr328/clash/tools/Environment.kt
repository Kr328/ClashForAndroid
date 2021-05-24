package com.github.kr328.clash.tools

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import java.io.File

class Environment(
    private val ndkDirectory: File,
    private val minSdkVersion: Int,
) {
    fun ofCoreBuild(abi: NativeAbi): Map<String, String> {
        val host = when {
            Os.isFamily(Os.FAMILY_WINDOWS) ->
                "windows"
            Os.isFamily(Os.FAMILY_MAC) ->
                "darwin"
            Os.isFamily(Os.FAMILY_UNIX) ->
                "linux"
            else ->
                throw GradleException("Unsupported host: ${System.getProperty("os.name")}")
        }

        val compiler = ndkDirectory.resolve("toolchains/llvm/prebuilt/$host-x86_64/bin")
            .resolve("${abi.compiler}${minSdkVersion}-clang")

        return mapOf(
            "CC" to compiler.absolutePath.toString().replace("\\", "/"),
            "GOOS" to "android",
            "GOARCH" to abi.goArch,
            "GOARM" to abi.goArm,
            "CGO_ENABLED" to "1",
            "CFLAGS" to "-O3 -Werror",
        )
    }

    fun ofLwipBuild(abi: NativeAbi): Map<String, String> {
        return mapOf(
            "CMAKE_SYSTEM_NAME" to "Android",
            "CMAKE_ANDROID_NDK" to ndkDirectory.absolutePath.toString().replace("\\", "/"),
            "CMAKE_ANDROID_ARCH_ABI" to abi.value,
            "CMAKE_SYSTEM_VERSION" to minSdkVersion.toString()
        )
    }
}