plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

allprojects {
    group = "io.getstream.chat"
    version = "1.0.0"

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.multiplatform")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
}

tasks.register("ktlintCheck") {
    group = "verification"
    description = "Check Kotlin code style"
    dependsOn(subprojects.map { it.tasks.matching { task -> task.name == "ktlintCheck" } })
}

tasks.register("ktlintFormat") {
    group = "formatting"
    description = "Format Kotlin code"
    dependsOn(subprojects.map { it.tasks.matching { task -> task.name == "ktlintFormat" } })
}

tasks.register("testAll") {
    group = "verification"
    description = "Run all tests"
    dependsOn(subprojects.map { it.tasks.matching { task -> task.name.contains("test") } })
}

tasks.register("buildAll") {
    group = "build"
    description = "Build all targets"
    dependsOn(subprojects.map { it.tasks.matching { task -> task.name == "build" } })
} 