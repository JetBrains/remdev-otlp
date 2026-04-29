package com.jetbrains.otp.exporter

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.otp.DiagnosticPlugin
import com.jetbrains.otp.span.CommonSpanAttributes
import com.jetbrains.otp.span.CommonSpanAttributesState

class BackendOtlpInitializer : ProjectActivity {
    override suspend fun execute(project: Project) {
        val config = OtlpConfigFactory.fromEnv()
        val commonAttributes = mutableMapOf(
            CommonSpanAttributes.RD_SIDE to CommonSpanAttributes.SIDE_BACKEND
        )
        DiagnosticPlugin.version()?.let { commonAttributes[CommonSpanAttributes.PLUGIN_VERSION] = it }
        CommonSpanAttributesState.upsert(commonAttributes)

        CpuUsageMetricReporter.getInstance().start(CommonSpanAttributes.SIDE_BACKEND)
        FrequentPerformanceMetricsReporter.getInstance().start(CommonSpanAttributes.SIDE_BACKEND)
        TelemetrySpanExporter.getInstance().initExporter(config)
        TelemetryMetricExporter.getInstance().initExporter(config)
    }
}
