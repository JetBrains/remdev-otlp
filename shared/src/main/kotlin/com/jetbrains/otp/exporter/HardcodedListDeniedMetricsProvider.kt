package com.jetbrains.otp.exporter

class HardcodedListDeniedMetricsProvider : DeniedMetricsProvider {
    override fun getDeniedMetrics(): List<String> {
        return DENIED_METRICS
    }
}

/**
 * Denylist of metric name patterns that should not be exported to OTLP backend.
 * Supports wildcards (*) for pattern matching.
 *
 * These metrics are typically noisy (always 0) or high-cardinality without value,
 * and blocking them reduces telemetry volume significantly at scale.
 */
private val DENIED_METRICS = listOf(
    // StreamlinedBlobStorage metrics - always 0, not useful
    "StreamlinedBlobStorage.*",

    // File cache metrics - high frequency, low value (mostly 0s)
    "FileNameCache.*",
    "FilePageCache.*",

    // Disk query metrics - mostly 0s unless actively indexing
    "DiskQueryRelay.*",

    // File channel retry metrics - rarely non-zero
    "FileChannelInterruptsRetryer.*",

    // Content storage deduplication - rarely changes
    "VFS.contentStorage.recordsDeduplicated",
    "VFS.contentStorage.recordsDecompressionTimeUs",

    // Cache state storage - very noisy when 0
    "cacheStateStorage.*",

    // Direct buffer allocator - reclaimed/disposed are rarely non-zero
    "DirectByteBufferAllocator.disposed",
    "DirectByteBufferAllocator.reclaimed",
)
