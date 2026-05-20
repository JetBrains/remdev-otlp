package com.jetbrains.otp.settings

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.otp.api.OtpDiagnosticSettingsApi

internal class BackendOtpDiagnosticSettingsApiImpl : OtpDiagnosticSettingsApi {
    override suspend fun getFrontendXmxMb(): Int? {
        val rawValue = System.getProperty(BACKEND_FRONTEND_XMX_PROPERTY_NAME)
        return parseFrontendXmxMb(rawValue).also { parsedValue ->
            if (rawValue != null && parsedValue == null) {
                LOG.warn(
                    "Ignoring invalid $BACKEND_FRONTEND_XMX_PROPERTY_NAME value: '$rawValue'. " +
                            "Expected a positive integer in MiB."
                )
            }
        }
    }

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

    companion object {
        private const val BACKEND_FRONTEND_XMX_PROPERTY_NAME = "rdct.diagnostic.frontend.xmx.mb"
        private val LOG = Logger.getInstance(BackendOtpDiagnosticSettingsApiImpl::class.java)

        private fun parseFrontendXmxMb(rawValue: String?): Int? {
            val trimmedValue = rawValue?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val parsedValue = trimmedValue.toIntOrNull() ?: return null
            return parsedValue.takeIf { it > 0 }
        }
    }
}
