package com.jetbrains.otp.exporter

import com.intellij.openapi.diagnostic.Logger
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.InstrumentType
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.MetricExporter

/**
 * Wraps another MetricExporter and filters out denied metrics based on denylist patterns.
 */
class DenylistFilteringMetricExporter(
    private val delegate: MetricExporter
) : MetricExporter {

    override fun export(metrics: Collection<MetricData>): CompletableResultCode {
        val filteredMetrics = metrics.filter { metric ->
            MetricNameFilter.shouldExport(metric.name)
        }

        val deniedCount = metrics.size - filteredMetrics.size
        if (deniedCount > 0) {
            LOG.debug("Filtered out $deniedCount denied metrics, exporting ${filteredMetrics.size} metrics")
        }

        if (filteredMetrics.isEmpty() && metrics.isNotEmpty()) {
            LOG.debug("All ${metrics.size} metrics were denied, skipping export")
            return CompletableResultCode.ofSuccess()
        }

        return delegate.export(filteredMetrics)
    }

    companion object {
        private val LOG = Logger.getInstance(DenylistFilteringMetricExporter::class.java)
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
