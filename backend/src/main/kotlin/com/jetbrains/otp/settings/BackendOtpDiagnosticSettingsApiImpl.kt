package com.jetbrains.otp.settings

import com.jetbrains.otp.api.OtpDiagnosticSettingsApi

internal class BackendOtpDiagnosticSettingsApiImpl : OtpDiagnosticSettingsApi {
    override suspend fun syncFilteringSettings(
        disabledCategories: Set<String>,
        frequentSpansEnabled: Boolean,
        pluginSpanFilterEnabled: Boolean,
        metricsExportEnabled: Boolean,
        frequentPerformanceMetricsReportingEnabled: Boolean,
    ) {
        OtpDiagnosticSettings.getInstance().syncFilteringSettings(
            disabledCategories = disabledCategories,
            frequentSpansEnabled = frequentSpansEnabled,
            pluginSpanFilterEnabled = pluginSpanFilterEnabled,
            metricsExportEnabled = metricsExportEnabled,
            frequentPerformanceMetricsReportingEnabled = frequentPerformanceMetricsReportingEnabled,
        )
    }
}
