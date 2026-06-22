import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.ProductMode

version = "1.0.23.2"
plugins {
    id("intellij-platform-remdev")
    alias(libs.plugins.rpc)
}

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaUltimate, libs.versions.ijPlatformFrontend) {
            productMode = ProductMode.FRONTEND
            useCache = false
            useInstaller = true
        }
        pluginModule(implementation(project(":shared")))
        bundledModules(
            "intellij.platform.frontend",
            "intellij.platform.frontend.split",
            "intellij.platform.frontend.split.connection",
            "intellij.platform.split.connection",
            "intellij.rd.client",
            "intellij.rd.client.base",
            "intellij.rd.platform",
        )
    }
}
