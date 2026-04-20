package com.jetbrains.otp.exporter

import io.opentelemetry.sdk.metrics.data.MetricData

fun interface MetricExportFilter {
    fun shouldExport(metric: MetricData): Boolean
}

open class PrefixMetricFilter(
    private val prefixes: Collection<String>
) : MetricExportFilter {
    override fun shouldExport(metric: MetricData): Boolean {
        return prefixes.none(metric.name::startsWith)
    }
}

object ServerSocketWrapperBytesMetricFilter : PrefixMetricFilter(
    prefixes = listOf(
        "rdct.ServerSocketWrapper.sentBytes.",
        "rdct.ServerSocketWrapper.receivedBytes."
    )
)
