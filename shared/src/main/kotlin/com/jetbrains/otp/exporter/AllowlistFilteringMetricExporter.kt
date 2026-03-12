package com.jetbrains.otp.exporter

import com.intellij.openapi.diagnostic.Logger
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.InstrumentType
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.MetricExporter

/**
 * Wraps another MetricExporter and filters metrics based on allowlist patterns.
 */
class AllowlistFilteringMetricExporter(
    private val delegate: MetricExporter
) : MetricExporter {

    override fun export(metrics: Collection<MetricData>): CompletableResultCode {
        val filteredMetrics = metrics.filter { metric ->
            MetricNameFilter.shouldExport(metric.name)
        }

        val filteredCount = metrics.size - filteredMetrics.size
        if (filteredCount > 0) {
            LOG.debug("Filtered out $filteredCount metrics by allowlist, exporting ${filteredMetrics.size} metrics")
        }

        if (filteredMetrics.isEmpty() && metrics.isNotEmpty()) {
            LOG.debug("All ${metrics.size} metrics were filtered out by allowlist, skipping export")
            return CompletableResultCode.ofSuccess()
        }

        return delegate.export(filteredMetrics)
    }

    companion object {
        private val LOG = Logger.getInstance(AllowlistFilteringMetricExporter::class.java)
    }

    override fun flush(): CompletableResultCode {
        return delegate.flush()
    }

    override fun shutdown(): CompletableResultCode {
        return delegate.shutdown()
    }

    override fun getAggregationTemporality(instrumentType: InstrumentType): AggregationTemporality {
        return delegate.getAggregationTemporality(instrumentType)
    }
}
