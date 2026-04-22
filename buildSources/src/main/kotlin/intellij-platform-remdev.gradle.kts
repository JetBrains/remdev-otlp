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

    // Use platform's exporter infrastructure to avoid ServiceLoader conflicts
    compileOnly("io.opentelemetry:opentelemetry-exporter-common:${SharedDependencies.OPEN_TELEMETRY_LIBRARY}")
    compileOnly("io.opentelemetry:opentelemetry-exporter-otlp-common:${SharedDependencies.OPEN_TELEMETRY_LIBRARY}")
    compileOnly("io.opentelemetry:opentelemetry-exporter-sender-jdk:${SharedDependencies.OPEN_TELEMETRY_LIBRARY}")

    implementation("io.opentelemetry:opentelemetry-exporter-otlp:${SharedDependencies.OPEN_TELEMETRY_LIBRARY}") {
        // Exclude SDK modules - use platform's versions
        exclude(group = "io.opentelemetry", module = "opentelemetry-sdk")
        exclude(group = "io.opentelemetry", module = "opentelemetry-sdk-trace")
        exclude(group = "io.opentelemetry", module = "opentelemetry-sdk-common")
        exclude(group = "io.opentelemetry", module = "opentelemetry-sdk-metrics")
        exclude(group = "io.opentelemetry", module = "opentelemetry-sdk-logs")
        exclude(group = "io.opentelemetry", module = "opentelemetry-api")
        exclude(group = "io.opentelemetry", module = "opentelemetry-context")

        // Exclude exporter infrastructure - use platform's versions to avoid ServiceLoader conflicts
        exclude(group = "io.opentelemetry", module = "opentelemetry-exporter-common")
        exclude(group = "io.opentelemetry", module = "opentelemetry-exporter-otlp-common")

        // Exclude HTTP sender implementations - use platform's JDK-based sender
        exclude(group = "io.opentelemetry", module = "opentelemetry-exporter-sender-okhttp")
        exclude(group = "io.opentelemetry", module = "opentelemetry-exporter-sender-jdk")
        exclude(group = "com.squareup.okhttp3")
        exclude(group = "com.squareup.okio")

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