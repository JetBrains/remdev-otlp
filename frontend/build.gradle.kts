import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.ProductMode

version = "1.0"
plugins {
    id("intellij-platform-remdev")
}

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaUltimate, libs.versions.ij.platform) {
            productMode = ProductMode.FRONTEND
        }
        pluginModule(implementation(project(":shared")))
        bundledModules(
            "intellij.platform.frontend",
        )
    }
}