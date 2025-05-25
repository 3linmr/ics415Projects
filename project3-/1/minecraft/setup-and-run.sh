#!/bin/bash

echo "=== Setting up Minecraft Game Engine ==="

# Make sure we have the src directory structure
mkdir -p src/main/java

# Move all Java files to the src/main/java directory if they're in the root
find . -maxdepth 1 -name "*.java" -exec mv {} src/main/java/ \;

# Check if the required files exist in src/main/java
FILES=(
    "MinecraftEngine.java"
    "Block.java"
    "Chunk.java"
    "World.java"
    "Camera.java"
)

MISSING=0
for FILE in "${FILES[@]}"; do
    if [ ! -f "src/main/java/$FILE" ]; then
        echo "ERROR: Missing file src/main/java/$FILE"
        MISSING=1
    else
        echo "Found src/main/java/$FILE"
    fi
done

if [ $MISSING -eq 1 ]; then
    echo "Some required files are missing. Please make sure all Java files are in src/main/java/"
    exit 1
fi

# Replace the build.gradle
echo "Updating build.gradle..."
cp build.gradle build.gradle.backup 2>/dev/null
cat > build.gradle << 'EOF'
plugins {
    id 'java'
    id 'application'
}

// Basic project info
group = 'com.minecraft'
version = '1.0-SNAPSHOT'

// Set Java compatibility
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// Define repositories for dependencies
repositories {
    mavenCentral()
}

// Define LWJGL version
def lwjglVersion = "3.3.3"

// Define dependencies
dependencies {
    // LWJGL Core
    implementation "org.lwjgl:lwjgl:${lwjglVersion}"
    implementation "org.lwjgl:lwjgl-glfw:${lwjglVersion}"
    implementation "org.lwjgl:lwjgl-opengl:${lwjglVersion}"

    // Determine the native library suffix based on OS and architecture
    def osName = System.getProperty("os.name").toLowerCase()
    def architecture = System.getProperty("os.arch").toLowerCase()
    def nativeSuffix

    if (osName.contains("mac") || osName.contains("darwin")) {
        if (architecture.contains("aarch64")) {
            nativeSuffix = "natives-macos-arm64"
            println "Detected macOS on ARM64"
        } else {
            nativeSuffix = "natives-macos"
            println "Detected macOS on x86_64"
        }
    } else if (osName.contains("win")) {
        nativeSuffix = "natives-windows"
        println "Detected Windows"
    } else if (osName.contains("linux")) {
        nativeSuffix = "natives-linux"
        println "Detected Linux"
    } else {
        throw new GradleException("Unsupported operating system: ${osName}")
    }

    // Add native libraries
    runtimeOnly "org.lwjgl:lwjgl:${lwjglVersion}:${nativeSuffix}"
    runtimeOnly "org.lwjgl:lwjgl-glfw:${lwjglVersion}:${nativeSuffix}"
    runtimeOnly "org.lwjgl:lwjgl-opengl:${lwjglVersion}:${nativeSuffix}"
}

// Define the main class
application {
    mainClass = 'MinecraftEngine'
}

// Add special JVM arguments for macOS
if (System.getProperty("os.name").toLowerCase().contains("mac")) {
    run {
        jvmArgs += [
            "-XstartOnFirstThread",
            "--add-opens", "java.base/java.nio=ALL-UNNAMED",
            "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
            "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED",
            "--add-opens", "java.desktop/sun.java2d=ALL-UNNAMED"
        ]
        println "Added macOS-specific JVM arguments"
    }
}
EOF

echo "Building and running the project..."
gradle clean
gradle run

echo "If you see errors above, try running: gradle run --stacktrace"