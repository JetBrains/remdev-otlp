package com.jetbrains.otp.exporter

import com.intellij.platform.diagnostic.telemetry.FilteredMetricsExporter
import com.intellij.platform.diagnostic.telemetry.impl.OpenTelemetryExporterProvider
import com.intellij.util.concurrency.SynchronizedClearableLazy
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.sdk.metrics.export.MetricExporter

@Suppress("UnstableApiUsage")
abstract class FilteredMetricExporterProvider : OpenTelemetryExporterProvider {
    override fun getMetricsExporters(): List<MetricExporter> {
        val allowedMetrics = AllowedMetricsProvider.EP_NAME.extensions.flatMap { it.getAllowedMetrics() }
        val exporter = FilteredMetricsExporter(
            underlyingExporter = SynchronizedClearableLazy { getUnderlyingExporter() },
            predicate = { true })
        return listOf(exporter)
    }

    abstract fun getUnderlyingExporter(): MetricExporter
}

