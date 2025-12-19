package com.jetbrains.otp.exporter

class HardcodedListAllowedMetricsProvider : AllowedMetricsProvider {
    override fun getAllowedMetrics(): List<String> {
        return METRICS
    }
}

private val METRICS = listOf("")
