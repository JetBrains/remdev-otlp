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

        // Build export pipeline: throttle → denylist → underlying exporter
        val pipelineExporter = SynchronizedClearableLazy<MetricExporter> {
            var exporter: MetricExporter = getUnderlyingExporter()

            // Apply denylist filtering if enabled
            if (diagnosticSettings.metricsDenylistEnabledEffective()) {
                exporter = DenylistFilteringMetricExporter(exporter)
            }

            // Apply throttling based on configured interval
            val intervalMinutes = diagnosticSettings.metricsExportIntervalMinutesEffective()
            val intervalMillis = intervalMinutes * 60 * 1000L
            exporter = ThrottledMetricExporter(exporter, intervalMillis)

            exporter
        }

        // Wrap with global enable/disable filtering
        val exporter = FilteredMetricsExporter(
            underlyingExporter = pipelineExporter,
            predicate = { diagnosticSettings.metricsExportEnabledEffective() })
        return listOf(exporter)
    }

    abstract fun getUnderlyingExporter(): MetricExporter
}