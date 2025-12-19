package com.jetbrains.otp.exporter

import com.intellij.openapi.diagnostic.Logger
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.InstrumentType
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.MetricExporter
import kotlin.collections.forEach

class BackendMetricExporter : FilteredMetricExporterProvider() {
    override fun getUnderlyingExporter(): MetricExporter {
        return LogExporter()
    }
}

class LogExporter : MetricExporter {
    companion object {
        private val LOG = Logger.getInstance(LogExporter::class.java)
    }

    override fun export(metrics: Collection<MetricData>): CompletableResultCode {
        metrics.forEach { LOG.info("Backend metric received ${it.name} ${it.data}") }
        return CompletableResultCode.ofSuccess()
    }

    override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override fun getAggregationTemporality(instrumentType: InstrumentType): AggregationTemporality =
        AggregationTemporality.DELTA
}