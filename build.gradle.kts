import com.jetbrains.otp.build.InlineModuleDescriptorsTask
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.platform.gradle.tasks.aware.SplitModeAware.SplitModeTarget

plugins {
    id("intellij-platform-main")
}

group = "com.jetbrains.otp"
version = "1.0.10"

val moduleDescriptorFiles = mapOf(
    "OtpDiagnostic.shared" to layout.projectDirectory.file("shared/src/main/resources/OtpDiagnostic.shared.xml"),
    "OtpDiagnostic.frontend" to layout.projectDirectory.file("frontend/src/main/resources/OtpDiagnostic.frontend.xml"),
    "OtpDiagnostic.backend" to layout.projectDirectory.file("backend/src/main/resources/OtpDiagnostic.backend.xml"),
)

val generatedRootPluginXml = layout.buildDirectory.file("generated/pluginXml/plugin.xml")

val inlineModuleDescriptorsIntoPluginXml by tasks.registering(InlineModuleDescriptorsTask::class) {
    rootPluginXml.set(layout.projectDirectory.file("src/main/resources/META-INF/plugin.xml"))
    outputFile.set(generatedRootPluginXml)

    moduleDescriptorFiles.forEach { (moduleName, descriptorFile) ->
        moduleDescriptors.put(moduleName, providers.fileContents(descriptorFile).asText)
    }
}

tasks.named<PatchPluginXmlTask>("patchPluginXml") {
    dependsOn(inlineModuleDescriptorsIntoPluginXml)
    inputFile.set(generatedRootPluginXml)
}

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaUltimate, libs.versions.ij.platform) {
            useInstaller = false
        }
        pluginModule(implementation(project(":shared")))
        pluginModule(implementation(project(":frontend")))
        pluginModule(implementation(project(":backend")))
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    splitMode = true
    splitModeTarget = SplitModeTarget.BOTH

    pluginVerification {
        ides {
            create(IntelliJPlatformType.IntellijIdeaUltimate, libs.versions.ij.platform)
        }
    }
}

tasks {
    runIde {
         systemProperty("rdct.diagnostic.otlp", "true")
         systemProperty("idea.diagnostic.opentelemetry.otlp", "true")
         systemProperty("otel.exporter.otlp.endpoint", System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT") ?: "http://localhost")
    }
}
