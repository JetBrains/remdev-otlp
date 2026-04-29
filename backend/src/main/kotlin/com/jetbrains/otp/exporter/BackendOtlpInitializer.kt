package com.jetbrains.otp.exporter

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.otp.span.CommonSpanAttributes
import com.jetbrains.otp.span.CommonSpanAttributesInitializer

class BackendOtlpInitializer : ProjectActivity {
    override suspend fun execute(project: Project) {
        val config = OtlpConfigFactory.fromEnv()
        CommonSpanAttributesInitializer.initialize(CommonSpanAttributes.SIDE_BACKEND)
        CpuUsageMetricReporter.getInstance().start(CommonSpanAttributes.SIDE_BACKEND)
        FrequentPerformanceMetricsReporter.getInstance().start(CommonSpanAttributes.SIDE_BACKEND)
        TelemetrySpanExporter.getInstance().initExporter(config)
        TelemetryMetricExporter.getInstance().initExporter(config)
    }
}
