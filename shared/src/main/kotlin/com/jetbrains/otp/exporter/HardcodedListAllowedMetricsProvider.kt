package com.jetbrains.otp.exporter

class HardcodedListAllowedMetricsProvider : AllowedMetricsProvider {
    override fun getAllowedMetrics(): List<String> {
        return METRICS
    }
}

/**
 * Allowlist of metrics that should be exported by this plugin.
 * Currently lists all metrics created by this plugin (rdct.* namespace).
 *
 * Note: This allowlist is not currently enforced in the export pipeline.
 * Metric filtering is primarily done via DeniedMetricsProvider (denylist).
 */
private val METRICS = listOf(
    // Real-time CPU utilization metrics
    "rdct.process.cpu.utilization",
    "rdct.system.cpu.utilization",

    // 5-minute window CPU metrics (on-demand during incidents)
    "rdct.process.cpu.utilization.window.samples",
    "rdct.process.cpu.utilization.window.avg",
    "rdct.process.cpu.utilization.window.max",
    "rdct.system.cpu.utilization.window.samples",
    "rdct.system.cpu.utilization.window.avg",
    "rdct.system.cpu.utilization.window.max",

    // 5-minute window memory metrics (on-demand during incidents)
    "rdct.process.memory.used.window.samples",
    "rdct.process.memory.used.window.avg",
    "rdct.process.memory.used.window.max",
    "rdct.system.memory.used.window.samples",
    "rdct.system.memory.used.window.avg",
    "rdct.system.memory.used.window.max",
)
