plugins {
    id("org.jetbrains.intellij.platform.module")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("rpc")
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
        exclude(group = "io.opentelemetry", module = "opentelemetry-sdk")
        exclude(group = "io.opentelemetry", module = "opentelemetry-sdk-trace")
        exclude(group = "io.opentelemetry", module = "opentelemetry-sdk-common")
        exclude(group = "io.opentelemetry", module = "opentelemetry-sdk-metrics")
        exclude(group = "io.opentelemetry", module = "opentelemetry-api")
        exclude(group = "io.opentelemetry", module = "opentelemetry-context")
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
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
}