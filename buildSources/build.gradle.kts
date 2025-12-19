plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies/")
}

dependencies {
    implementation(libs.intellij.platform.gradle)
    implementation(libs.rpc.gradle)
    implementation(libs.kotlin.gradle)
    implementation(libs.kotlin.serialization)
}