import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.ProductMode

version = "1.0.15.3"

plugins {
    id("intellij-platform-remdev")
}

dependencies {
    val localSplitConnectionClasses = rootProject.layout.projectDirectory
        .dir("../ultimate/out/classes/production/intellij.platform.split.connection")

    if (localSplitConnectionClasses.asFile.exists()) {
        compileOnly(files(localSplitConnectionClasses))
    }

    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaUltimate, libs.versions.ij.platform) {
            productMode = ProductMode.BACKEND
        }
        pluginModule(implementation(project(":shared")))
        bundledModule("intellij.platform.rpc.backend")
        bundledModule("intellij.platform.backend")
        bundledPlugin("com.jetbrains.codeWithMe")
    }
}
