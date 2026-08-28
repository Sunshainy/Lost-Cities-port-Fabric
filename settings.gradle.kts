pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.7"
    // Объявляем версию Loom здесь, применяем в build.gradle.kts каждой версии.
    id("net.fabricmc.fabric-loom-remap") version "1.15-SNAPSHOT" apply false
}

stonecutter {
    create(rootProject) {
        // Версии добавляются по одной, по мере того как каждая проверена сборкой и запуском.
        // Целевой диапазон: 1.20.1, 1.20.4, 1.20.6, 1.21.1, 1.21.4, 1.21.8, 1.21.11.
        versions("1.20.1", "1.20.4", "1.20.6", "1.21.1", "1.21.4")
        vcsVersion = "1.20.1"
    }
}

rootProject.name = "lostcities"
