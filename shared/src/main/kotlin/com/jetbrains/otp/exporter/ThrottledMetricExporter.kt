package com.jetbrains.otp.exporter

import com.intellij.openapi.diagnostic.Logger
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.InstrumentType
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.MetricExporter
import java.util.concurrent.atomic.AtomicLong

/**
 * Wraps a MetricExporter and throttles exports to reduce backend load.
 *
 * Platform collects metrics every ~1 minute, but this exporter only sends to
 * backend every [exportIntervalMillis] to avoid rate limits.
 *
 * Example: Platform calls export() every 60s, but with 5min interval,
 * actual OTLP exports happen every 5 minutes.
 */
class ThrottledMetricExporter(
    private val delegate: MetricExporter,
    private val exportIntervalMillis: Long
) : MetricExporter {

    private val lastExportTimeMillis = AtomicLong(0L)

    override fun export(metrics: Collection<MetricData>): CompletableResultCode {
        val now = System.currentTimeMillis()
        val lastExport = lastExportTimeMillis.get()
        val timeSinceLastExport = now - lastExport

        // First export or enough time has passed
        if (lastExport == 0L || timeSinceLastExport >= exportIntervalMillis) {
            if (lastExportTimeMillis.compareAndSet(lastExport, now)) {
                LOG.debug("Exporting ${metrics.size} metrics (${timeSinceLastExport}ms since last export)")
                return delegate.export(metrics)
            }
        }

        // Skip this export - too soon
        LOG.debug("Throttling metric export (${timeSinceLastExport}ms < ${exportIntervalMillis}ms)")
        return CompletableResultCode.ofSuccess()
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

    companion object {
        private val LOG = Logger.getInstance(ThrottledMetricExporter::class.java)
    }
}
