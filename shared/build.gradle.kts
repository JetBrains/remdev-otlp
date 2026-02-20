import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

version = "1.0.7"
plugins {
    id("intellij-platform-remdev")
}
dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaUltimate, libs.versions.ij.platform) {
            useInstaller = false
        }
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
}