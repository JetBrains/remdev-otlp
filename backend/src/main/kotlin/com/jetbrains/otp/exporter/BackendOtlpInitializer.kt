package com.jetbrains.otp.exporter

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.otp.span.CommonSpanAttributes
import com.jetbrains.otp.span.CommonSpanAttributesState

class BackendOtlpInitializer : ProjectActivity {
    override suspend fun execute(project: Project) {
        CommonSpanAttributesState.put(CommonSpanAttributes.RD_SIDE, CommonSpanAttributes.SIDE_BACKEND)
        val config = OtlpConfigFactory.fromEnv()
        TelemetrySpanExporter.getInstance().initExporter(config)
        TelemetryMetricExporter.getInstance().initExporter(config)
    }
}
