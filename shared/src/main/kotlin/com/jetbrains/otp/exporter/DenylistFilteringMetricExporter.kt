package com.jetbrains.otp.exporter

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
        return delegate.export(filteredMetrics)
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
