package com.jetbrains.otp.exporter

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.otp.DiagnosticPlugin
import com.jetbrains.otp.exporter.processor.BufferingWrapperProcessor
import com.jetbrains.otp.span.CommonSpanAttributes
import com.jetbrains.otp.span.CommonSpanAttributesState

class FrontendOtlpInitializer : ProjectActivity {
    override suspend fun execute(project: Project) {
        try {
            val config = OtlpConfigProvider.EP_NAME.extensionList.firstOrNull()?.createConfig()
                ?: OtlpConfigFactory.fromEnv()

            val commonAttributes = mutableMapOf(CommonSpanAttributes.RD_SIDE to CommonSpanAttributes.SIDE_FRONTEND)
            DiagnosticPlugin.version()?.let { commonAttributes[CommonSpanAttributes.PLUGIN_VERSION] = it }
            CommonSpanAttributesState.upsert(commonAttributes)

            CpuUsageMetricReporter.getInstance().start(CommonSpanAttributes.SIDE_FRONTEND)
            FrequentPerformanceMetricsReporter.getInstance().start(CommonSpanAttributes.SIDE_FRONTEND)

            TelemetrySpanExporter.getInstance().initExporter(config)
            TelemetryMetricExporter.getInstance().initExporter(config)

            BufferingWrapperProcessor.onCryptoReady()
        } catch (_: Exception) {
            BufferingWrapperProcessor.onCryptoReady()
        }
    }
}
