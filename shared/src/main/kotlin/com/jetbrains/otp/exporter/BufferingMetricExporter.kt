package com.jetbrains.otp.exporter

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.InstrumentType
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.MetricExporter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

class BufferingMetricExporter : MetricExporter {
    private val buffer = ConcurrentLinkedQueue<MetricData>()
    private val delegate = AtomicReference<MetricExporter?>(null)

    override fun export(metrics: Collection<MetricData>): CompletableResultCode {
        val filteredMetrics = metrics.filterNot(::shouldDropMetric)
        if (filteredMetrics.isEmpty()) {
            return CompletableResultCode.ofSuccess()
        }

        val exporter = delegate.get()
        return if (exporter != null) {
            exporter.export(filteredMetrics)
        } else {
            buffer.addAll(filteredMetrics)
            CompletableResultCode.ofSuccess()
        }
    }

    fun setDelegate(exporter: MetricExporter) {
        delegate.set(exporter)
        drainBuffer(exporter)
    }

    private fun drainBuffer(exporter: MetricExporter) {
        val buffered = mutableListOf<MetricData>()
        while (true) {
            val metric = buffer.poll() ?: break
            if (!shouldDropMetric(metric)) {
                buffered.add(metric)
            }
        }

        if (buffered.isNotEmpty()) {
            exporter.export(buffered)
        }
    }

    override fun getAggregationTemporality(instrumentType: InstrumentType): AggregationTemporality {
        return delegate.get()?.getAggregationTemporality(instrumentType)
            ?: AggregationTemporality.CUMULATIVE
    }

    override fun flush(): CompletableResultCode {
        return delegate.get()?.flush() ?: CompletableResultCode.ofSuccess()
    }

    override fun shutdown(): CompletableResultCode {
        return delegate.get()?.shutdown() ?: CompletableResultCode.ofSuccess()
    }

    private fun shouldDropMetric(metric: MetricData): Boolean {
        return METRIC_EXPORT_FILTERS.any { filter -> !filter.shouldExport(metric) }
    }

    companion object {
        private val METRIC_EXPORT_FILTERS = listOf(
            ServerSocketWrapperBytesMetricFilter
        )
    }
}