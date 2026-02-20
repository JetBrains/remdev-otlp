import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.ProductMode

version = "1.0.7"

plugins {
    id("intellij-platform-remdev")
}

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaUltimate, libs.versions.ij.platform) {
            productMode = ProductMode.BACKEND
        }
        pluginModule(implementation(project(":shared")))
        bundledModule("intellij.platform.rpc.backend")
        bundledModule("intellij.platform.backend")
    }
}