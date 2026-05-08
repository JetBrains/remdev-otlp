package com.jetbrains.otp.exporter

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.otp.exporter.processor.BufferingWrapperProcessor
import com.jetbrains.otp.span.CommonSpanAttributes
import com.jetbrains.otp.span.CommonSpanAttributesInitializer
import com.jetbrains.otp.span.CommonSpanAttributesState
import java.util.concurrent.TimeUnit

class FrontendOtlpInitializer : ProjectActivity {
    override suspend fun execute(project: Project) {
        try {
            val config = OtlpConfigProvider.EP_NAME.extensionList.firstOrNull()?.createConfig()
                ?: OtlpConfigFactory.fromEnv()

            CommonSpanAttributesInitializer.initialize(CommonSpanAttributes.SIDE_FRONTEND)
            hostNameFromCommand()?.let { hostName ->
                CommonSpanAttributesState.put(CommonSpanAttributes.HOST_NAME, hostName)
            }
            CpuUsageMetricReporter.getInstance().start(CommonSpanAttributes.SIDE_FRONTEND)
            FrequentPerformanceMetricsReporter.getInstance().start(CommonSpanAttributes.SIDE_FRONTEND)

            TelemetrySpanExporter.getInstance().initExporter(config)
            TelemetryMetricExporter.getInstance().initExporter(config)

            BufferingWrapperProcessor.onCryptoReady()
        } catch (_: Exception) {
            BufferingWrapperProcessor.onCryptoReady()
        }
    }

    private fun hostNameFromCommand(): String? {
        return try {
            val process = ProcessBuilder("hostname")
                .redirectErrorStream(true)
                .start()

            if (!process.waitFor(1, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return null
            }

            if (process.exitValue() != 0) return null

            process.inputStream.bufferedReader().use { reader ->
                reader.readText().trim().takeIf { it.isNotEmpty() }
            }
        } catch (_: Exception) {
            null
        }
    }
}
