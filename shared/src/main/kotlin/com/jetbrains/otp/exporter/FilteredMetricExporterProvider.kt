package com.jetbrains.otp.exporter

import com.intellij.platform.diagnostic.telemetry.FilteredMetricsExporter
import com.intellij.platform.diagnostic.telemetry.impl.OpenTelemetryExporterProvider
import com.intellij.util.concurrency.SynchronizedClearableLazy
import com.jetbrains.otp.settings.OtpDiagnosticSettings
import io.opentelemetry.sdk.metrics.export.MetricExporter

@Suppress("UnstableApiUsage")
abstract class FilteredMetricExporterProvider : OpenTelemetryExporterProvider {
    override fun getMetricsExporters(): List<MetricExporter> {
        val diagnosticSettings = OtpDiagnosticSettings.getInstance()
        val exporter = FilteredMetricsExporter(
            underlyingExporter = SynchronizedClearableLazy { getUnderlyingExporter() },
            predicate = { diagnosticSettings.metricsExportEnabledEffective() })
        return listOf(exporter)
    }

    abstract fun getUnderlyingExporter(): MetricExporter
}