package com.jetbrains.otp.exporter

import com.jetbrains.rd.platform.codeWithMe.unattendedHost.metrics.Metric
import com.jetbrains.rd.platform.codeWithMe.unattendedHost.metrics.MetricsStatus
import com.jetbrains.rdserver.unattendedHost.diagnostics.BackendDiagnosticsService
import com.jetbrains.rdserver.unattendedHost.metrics.CpuMetricsProvider
import com.jetbrains.rdserver.unattendedHost.metrics.DiskMetricsProvider
import com.jetbrains.rdserver.unattendedHost.metrics.MemoryMetricsProvider

internal class BackendPerformanceStatusProvider {
    fun getAlertStatuses(): List<PerformanceStatus> {
        val diagnosticsService = runCatching {
            BackendDiagnosticsService.getInstance()
        }.getOrNull() ?: return emptyList()

        return METRICS.mapNotNull { definition ->
            val metric = diagnosticsService.readMetric(definition.metricName) ?: return@mapNotNull null
            if (metric.status == MetricsStatus.NORMAL) return@mapNotNull null
            metric.toPerformanceStatus(definition)
        }
    }

    private fun BackendDiagnosticsService.readMetric(metricName: String): HostPerformanceMetric? {
        return runCatching { getMetric(metricName).toHostPerformanceMetric() }.getOrNull()
    }

    private fun Metric.toHostPerformanceMetric(): HostPerformanceMetric? {
        val numericValue = valueProperty.value as? Number ?: return null
        return HostPerformanceMetric(
            status = statusProperty.value,
            value = numericValue.toDouble(),
            message = toString().takeIf { it.isNotBlank() },
        )
    }

    private fun HostPerformanceMetric.toPerformanceStatus(definition: MetricDefinition): PerformanceStatus {
        return PerformanceStatus(
            metric = definition.metric,
            status = status,
            value = value,
            unit = definition.unit,
            message = message,
        )
    }

    private data class HostPerformanceMetric(
        val status: MetricsStatus,
        val value: Double,
        val message: String?,
    )

    private data class MetricDefinition(
        val metricName: String,
        val metric: PerformanceMetric,
        val unit: String,
    )

    private companion object {
        private const val PERCENT = "%"

        val METRICS = listOf(
            MetricDefinition(CpuMetricsProvider.SYSTEM_CPU_LOAD, PerformanceMetric.SYSTEM_CPU, PERCENT),
            MetricDefinition(MemoryMetricsProvider.USED_MEMORY_PERCENTAGE, PerformanceMetric.SYSTEM_MEMORY, PERCENT),
            MetricDefinition(
                MemoryMetricsProvider.USED_JVM_MEMORY_PERCENTAGE,
                PerformanceMetric.JVM_MEMORY,
                PERCENT
            ),
            MetricDefinition(DiskMetricsProvider.USED_DISK_SPACE_PERCENTAGE, PerformanceMetric.DISK, PERCENT),
            MetricDefinition(CpuMetricsProvider.LOAD_AVERAGE_1, PerformanceMetric.LOAD_AVERAGE, "1"),
        )
    }
}
