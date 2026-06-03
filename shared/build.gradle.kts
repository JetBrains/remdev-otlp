import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

version = "1.0.20"
plugins {
    id("intellij-platform-remdev")
    alias(libs.plugins.rpc)
}
dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaUltimate, libs.versions.ijPlatform) {
            useInstaller = true
            useCache = false
        }
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
}
