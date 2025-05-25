plugins {
    kotlin("jvm") version "1.9.10"
    application
}

group = "engine"
version = "1.0-SNAPSHOT"

val lwjglVersion = "3.3.2"
val lwjglNatives = when {
    org.gradle.internal.os.OperatingSystem.current().isWindows -> "natives-windows"
    org.gradle.internal.os.OperatingSystem.current().isLinux -> "natives-linux"
    System.getProperty("os.arch") == "aarch64" -> "natives-macos-arm64" // ✅ Apple Silicon (M1/M2)
    else -> "natives-macos"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    // Core LWJGL
    implementation("org.lwjgl:lwjgl")
    runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")

    // OpenGL
    implementation("org.lwjgl:lwjgl-opengl")
    runtimeOnly("org.lwjgl:lwjgl-opengl::$lwjglNatives")

    // GLFW
    implementation("org.lwjgl:lwjgl-glfw")
    runtimeOnly("org.lwjgl:lwjgl-glfw::$lwjglNatives")

    // STB (image loading)
    implementation("org.lwjgl:lwjgl-stb")
    runtimeOnly("org.lwjgl:lwjgl-stb::$lwjglNatives")

    // Math (JOML)
    implementation("org.joml:joml:1.10.5")
}

application {
    mainClass.set("engine.Main")
    applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}