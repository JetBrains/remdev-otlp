import com.jetbrains.otp.build.InlineModuleDescriptorsTask
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.ProductMode
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.platform.gradle.tasks.aware.SplitModeAware.PluginInstallationTarget

plugins {
    id("intellij-platform-main")
}

group = "com.jetbrains.otp"
version = "1.0.21.2"

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
    sinceBuild.set("262")
    untilBuild.set("262.*")
}

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaUltimate, libs.versions.ijPlatform) {
            productMode = ProductMode.BACKEND
            useCache = false
            useInstaller = true
        }
        pluginModule(implementation(project(":shared")))
        pluginModule(implementation(project(":frontend")))
        pluginModule(implementation(project(":backend")))
        bundledPlugin("com.jetbrains.remoteDevelopment")
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    splitMode = true
    pluginInstallationTarget = PluginInstallationTarget.BOTH

    pluginVerification {
        ides {
            create(IntelliJPlatformType.IntellijIdeaUltimate, libs.versions.ijPlatform) {
                productMode = ProductMode.BACKEND
                useInstaller = true
            }
        }
    }
}

tasks {
    runIde {
        val runIdeDiagnosticsDir = layout.buildDirectory.dir("runIde-diagnostics").get().asFile

        systemProperty("rdct.diagnostic.otlp", "true")
        systemProperty("idea.diagnostic.opentelemetry.otlp", "true")
        systemProperty("otel.exporter.otlp.endpoint", System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT") ?: "http://localhost")
        systemProperty("rdct.embedded.client.debug.port", "5010")
        systemProperty("rdct.embedded.client.debug.suspend", "false")
        systemProperty("user.dir", layout.projectDirectory.asFile.absolutePath)

        jvmArgs(
            "-XX:ErrorFile=${runIdeDiagnosticsDir.absolutePath}/hs_err_pid%p.log",
            "-XX:HeapDumpPath=${runIdeDiagnosticsDir.absolutePath}",
        )

        doFirst {
            runIdeDiagnosticsDir.mkdirs()
        }
    }
}
