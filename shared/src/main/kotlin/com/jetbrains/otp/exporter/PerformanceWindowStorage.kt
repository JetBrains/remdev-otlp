package com.jetbrains.otp.exporter

import java.util.ArrayDeque

internal class PerformanceWindowStorage(
    private val windowMillis: Long,
    private val samplePeriodMillis: Long,
) {
    private val lock = Any()
    private val samples = ArrayDeque<PerformanceSample>()

    fun addSample(sample: PerformanceSample) {
        synchronized(lock) {
            trimExpiredSamples(sample.epochMillis)
            samples.addLast(sample)
        }
    }

    fun snapshot(nowMillis: Long): PerformanceMetricsSnapshot {
        val snapshotSamples = synchronized(lock) {
            trimExpiredSamples(nowMillis)
            samples.toList()
        }

        val processCpuValues = snapshotSamples.mapNotNull { it.processCpuLoad }
        val systemCpuValues = snapshotSamples.mapNotNull { it.systemCpuLoad }
        val processUsedMemoryValues = snapshotSamples.mapNotNull { it.processUsedMemoryBytes }
        val systemUsedMemoryValues = snapshotSamples.mapNotNull { it.systemUsedMemoryBytes }

        return PerformanceMetricsSnapshot(
            fromEpochMillis = nowMillis - windowMillis,
            toEpochMillis = nowMillis,
            samplePeriodMillis = samplePeriodMillis,
            samples = snapshotSamples,
            processCpuAverage = processCpuValues.averageOrNullSafe(),
            processCpuMax = processCpuValues.maxOrNull(),
            systemCpuAverage = systemCpuValues.averageOrNullSafe(),
            systemCpuMax = systemCpuValues.maxOrNull(),
            processUsedMemoryAverageBytes = processUsedMemoryValues.averageOrNullSafe(),
            processUsedMemoryMaxBytes = processUsedMemoryValues.maxOrNull(),
            systemUsedMemoryAverageBytes = systemUsedMemoryValues.averageOrNullSafe(),
            systemUsedMemoryMaxBytes = systemUsedMemoryValues.maxOrNull(),
        )
    }

    fun clear() {
        synchronized(lock) {
            samples.clear()
        }
    }

    private fun trimExpiredSamples(nowMillis: Long) {
        val threshold = nowMillis - windowMillis
        while (samples.isNotEmpty() && samples.first().epochMillis < threshold) {
            samples.removeFirst()
        }
    }
}
