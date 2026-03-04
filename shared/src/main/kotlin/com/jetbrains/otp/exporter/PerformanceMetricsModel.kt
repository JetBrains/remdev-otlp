package com.jetbrains.otp.exporter

data class PerformanceSample(
    val epochMillis: Long,
    val processCpuLoad: Double?,
    val systemCpuLoad: Double?,
    val processUsedMemoryBytes: Double?,
    val systemUsedMemoryBytes: Double?,
)

data class PerformanceMetricsSnapshot(
    val fromEpochMillis: Long,
    val toEpochMillis: Long,
    val samplePeriodMillis: Long,
    val samples: List<PerformanceSample>,
    val processCpuAverage: Double?,
    val processCpuMax: Double?,
    val systemCpuAverage: Double?,
    val systemCpuMax: Double?,
    val processUsedMemoryAverageBytes: Double?,
    val processUsedMemoryMaxBytes: Double?,
    val systemUsedMemoryAverageBytes: Double?,
    val systemUsedMemoryMaxBytes: Double?,
)

internal fun List<Double>.averageOrNullSafe(): Double? {
    if (isEmpty()) return null
    return average()
}
