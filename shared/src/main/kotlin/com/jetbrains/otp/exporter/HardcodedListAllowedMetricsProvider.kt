package com.jetbrains.otp.exporter

class HardcodedListAllowedMetricsProvider : AllowedMetricsProvider {
    override fun getAllowedMetrics(): List<String> {
        return METRICS
    }
}

/**
 * Allowlist of metrics that should be exported.
 * Focuses on JVM, CPU, and performance-critical metrics.
 * Supports wildcard (*) matching.
 */
private val METRICS = listOf(
    // Plugin's own metrics
    "rdct.*",

    // JVM metrics (heap, GC, threads, CPU, memory)
    "JVM.*",

    // UI responsiveness
    "AWTEventQueue.*",

    // Performance-critical platform metrics
    "Indexes.*",
    "Indexing.*",
    "workspaceModel.*",
    "VFS.cache.*",
    "VFS.fileByIdCache.*",
    "VFS.rootsCount",
    "ReadAction.*",
    "WriteAction.*",
    "writeAction.*",
    "FlushQueue.*",
    "LowMemory.*",
    "MEM.*",
    "OS.loadAverage",
)
