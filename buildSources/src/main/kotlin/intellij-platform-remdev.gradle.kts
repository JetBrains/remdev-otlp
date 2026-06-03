plugins {
    id("org.jetbrains.intellij.platform.module")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:${SharedDependencies.KOTLIN_SERIALIZATION_LIBRARY}")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:${SharedDependencies.KOTLIN_SERIALIZATION_LIBRARY}")

    compileOnly("io.opentelemetry:opentelemetry-sdk:${SharedDependencies.OPEN_TELEMETRY_LIBRARY}")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:${SharedDependencies.OPEN_TELEMETRY_LIBRARY}") {
        isTransitive = false
    }
}

intellijPlatform {
    buildSearchableOptions = false
}

kotlin {
    jvmToolchain(SharedDependencies.JAVA_LANGUAGE_VERSION)
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "${SharedDependencies.JAVA_LANGUAGE_VERSION}"
        targetCompatibility = "${SharedDependencies.JAVA_LANGUAGE_VERSION}"
    }

    withType<Test> {
        exclude("**/*\$*")
    }
}
