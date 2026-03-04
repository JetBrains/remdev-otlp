package com.jetbrains.otp.exporter

import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory

internal interface PerformanceSampleCollector {
    fun collectSample(nowMillis: Long): PerformanceSample?
}

internal class JvmPerformanceSampleCollector(
    private val operatingSystemMxBean: OperatingSystemMXBean? = ManagementFactory.getOperatingSystemMXBean() as? OperatingSystemMXBean,
    private val runtime: Runtime = Runtime.getRuntime(),
) : PerformanceSampleCollector {
    override fun collectSample(nowMillis: Long): PerformanceSample? {
        val mxBean = operatingSystemMxBean
        val processCpuLoad = normalizeCpuLoad(mxBean?.processCpuLoad)
        val systemCpuLoad = normalizeCpuLoad(mxBean?.cpuLoad)
        val processUsedMemoryBytes = readProcessUsedMemoryBytes()
        val systemUsedMemoryBytes = readSystemUsedMemoryBytes(mxBean)

        if (processCpuLoad == null
            && systemCpuLoad == null
            && processUsedMemoryBytes == null
            && systemUsedMemoryBytes == null
        ) {
            return null
        }

        return PerformanceSample(
            epochMillis = nowMillis,
            processCpuLoad = processCpuLoad,
            systemCpuLoad = systemCpuLoad,
            processUsedMemoryBytes = processUsedMemoryBytes,
            systemUsedMemoryBytes = systemUsedMemoryBytes,
        )
    }

    private fun normalizeCpuLoad(rawCpuLoad: Double?): Double? {
        if (rawCpuLoad == null || !rawCpuLoad.isFinite()) return null
        return rawCpuLoad.takeIf { it in 0.0..1.0 }
    }

    private fun readProcessUsedMemoryBytes(): Double? {
        val used = runtime.totalMemory() - runtime.freeMemory()
        if (used < 0L) return null
        return used.toDouble()
    }

    private fun readSystemUsedMemoryBytes(mxBean: OperatingSystemMXBean?): Double? {
        if (mxBean == null) return null
        val total = mxBean.totalMemorySize
        val free = mxBean.freeMemorySize
        if (total <= 0L || free < 0L) return null
        return (total - free).coerceAtLeast(0L).toDouble()
    }
}
