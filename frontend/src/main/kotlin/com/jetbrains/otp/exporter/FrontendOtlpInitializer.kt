package com.jetbrains.otp.exporter

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.otp.exporter.processor.BufferingWrapperProcessor
import com.jetbrains.otp.span.CommonSpanAttributes
import com.jetbrains.otp.span.CommonSpanAttributesInitializer
import com.jetbrains.otp.span.CommonSpanAttributesState

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
            val commandLine = GeneralCommandLine("hostname").withRedirectErrorStream(true)
            val output = ExecUtil.execAndGetOutput(commandLine, 1_000)
            if (output.isTimeout || output.exitCode != 0) return null

            output.stdout.trim().takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }
}
