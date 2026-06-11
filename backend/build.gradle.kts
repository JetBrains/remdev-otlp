import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.ProductMode

version = "1.0.20"

plugins {
    id("intellij-platform-remdev")
    alias(libs.plugins.rpc)
}

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaUltimate, libs.versions.ijPlatform) {
            productMode = ProductMode.BACKEND
            useCache = false
            useInstaller = true
        }
        pluginModule(implementation(project(":shared")))
        bundledModule("intellij.platform.rpc.backend")
        bundledModule("intellij.platform.backend")
        bundledModule("intellij.rd.platform")
        bundledPlugin("com.jetbrains.remoteDevelopment")
    }
}
