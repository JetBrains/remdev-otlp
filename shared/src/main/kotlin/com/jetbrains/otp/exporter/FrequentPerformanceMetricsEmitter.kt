package com.jetbrains.otp.exporter

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.DoubleGauge
import io.opentelemetry.api.metrics.DoubleHistogram

internal class FrequentPerformanceMetricsEmitter {
    fun emit(snapshot: PerformanceMetricsSnapshot, samplesToReport: List<PerformanceSample>, attributes: Attributes) {
        samplesToReport.forEach { sample ->
            sample.processCpuLoad?.let { processCpuWindowHistogram.record(it, attributes) }
            sample.systemCpuLoad?.let { systemCpuWindowHistogram.record(it, attributes) }
            sample.processUsedMemoryBytes?.let { processUsedMemoryWindowHistogram.record(it, attributes) }
            sample.systemUsedMemoryBytes?.let { systemUsedMemoryWindowHistogram.record(it, attributes) }
        }

        snapshot.processCpuAverage?.let { processCpuWindowAverageGauge.set(it, attributes) }
        snapshot.processCpuMax?.let { processCpuWindowMaxGauge.set(it, attributes) }
        snapshot.systemCpuAverage?.let { systemCpuWindowAverageGauge.set(it, attributes) }
        snapshot.systemCpuMax?.let { systemCpuWindowMaxGauge.set(it, attributes) }
        snapshot.processUsedMemoryAverageBytes?.let { processUsedMemoryWindowAverageGauge.set(it, attributes) }
        snapshot.processUsedMemoryMaxBytes?.let { processUsedMemoryWindowMaxGauge.set(it, attributes) }
        snapshot.systemUsedMemoryAverageBytes?.let { systemUsedMemoryWindowAverageGauge.set(it, attributes) }
        snapshot.systemUsedMemoryMaxBytes?.let { systemUsedMemoryWindowMaxGauge.set(it, attributes) }
    }

    private val processCpuWindowHistogram: DoubleHistogram by lazy {
        meter.histogramBuilder(PROCESS_CPU_WINDOW_SAMPLES_METRIC)
            .setDescription("Process CPU utilization samples from the last 5-minute on-demand report")
            .setUnit("1")
            .build()
    }

    private val systemCpuWindowHistogram: DoubleHistogram by lazy {
        meter.histogramBuilder(SYSTEM_CPU_WINDOW_SAMPLES_METRIC)
            .setDescription("System CPU utilization samples from the last 5-minute on-demand report")
            .setUnit("1")
            .build()
    }

    private val processCpuWindowAverageGauge: DoubleGauge by lazy {
        meter.gaugeBuilder(PROCESS_CPU_WINDOW_AVERAGE_METRIC)
            .setDescription("Average process CPU utilization for the last 5-minute on-demand report")
            .setUnit("1")
            .build()
    }

    private val processCpuWindowMaxGauge: DoubleGauge by lazy {
        meter.gaugeBuilder(PROCESS_CPU_WINDOW_MAX_METRIC)
            .setDescription("Max process CPU utilization for the last 5-minute on-demand report")
            .setUnit("1")
            .build()
    }

    private val systemCpuWindowAverageGauge: DoubleGauge by lazy {
        meter.gaugeBuilder(SYSTEM_CPU_WINDOW_AVERAGE_METRIC)
            .setDescription("Average system CPU utilization for the last 5-minute on-demand report")
            .setUnit("1")
            .build()
    }

    private val systemCpuWindowMaxGauge: DoubleGauge by lazy {
        meter.gaugeBuilder(SYSTEM_CPU_WINDOW_MAX_METRIC)
            .setDescription("Max system CPU utilization for the last 5-minute on-demand report")
            .setUnit("1")
            .build()
    }

    private val processUsedMemoryWindowHistogram: DoubleHistogram by lazy {
        meter.histogramBuilder(PROCESS_USED_MEMORY_WINDOW_SAMPLES_METRIC)
            .setDescription("Process used memory samples from the last 5-minute on-demand report")
            .setUnit("By")
            .build()
    }

    private val systemUsedMemoryWindowHistogram: DoubleHistogram by lazy {
        meter.histogramBuilder(SYSTEM_USED_MEMORY_WINDOW_SAMPLES_METRIC)
            .setDescription("System used memory samples from the last 5-minute on-demand report")
            .setUnit("By")
            .build()
    }

    private val processUsedMemoryWindowAverageGauge: DoubleGauge by lazy {
        meter.gaugeBuilder(PROCESS_USED_MEMORY_WINDOW_AVERAGE_METRIC)
            .setDescription("Average process used memory for the last 5-minute on-demand report")
            .setUnit("By")
            .build()
    }

    private val processUsedMemoryWindowMaxGauge: DoubleGauge by lazy {
        meter.gaugeBuilder(PROCESS_USED_MEMORY_WINDOW_MAX_METRIC)
            .setDescription("Max process used memory for the last 5-minute on-demand report")
            .setUnit("By")
            .build()
    }

    private val systemUsedMemoryWindowAverageGauge: DoubleGauge by lazy {
        meter.gaugeBuilder(SYSTEM_USED_MEMORY_WINDOW_AVERAGE_METRIC)
            .setDescription("Average system used memory for the last 5-minute on-demand report")
            .setUnit("By")
            .build()
    }

    private val systemUsedMemoryWindowMaxGauge: DoubleGauge by lazy {
        meter.gaugeBuilder(SYSTEM_USED_MEMORY_WINDOW_MAX_METRIC)
            .setDescription("Max system used memory for the last 5-minute on-demand report")
            .setUnit("By")
            .build()
    }

    private val meter by lazy {
        GlobalOpenTelemetry.get().getMeter(METER_NAME)
    }

    companion object {
        const val PROCESS_CPU_WINDOW_SAMPLES_METRIC = "rdct.process.cpu.utilization.window.samples"
        const val SYSTEM_CPU_WINDOW_SAMPLES_METRIC = "rdct.system.cpu.utilization.window.samples"
        const val PROCESS_CPU_WINDOW_AVERAGE_METRIC = "rdct.process.cpu.utilization.window.avg"
        const val PROCESS_CPU_WINDOW_MAX_METRIC = "rdct.process.cpu.utilization.window.max"
        const val SYSTEM_CPU_WINDOW_AVERAGE_METRIC = "rdct.system.cpu.utilization.window.avg"
        const val SYSTEM_CPU_WINDOW_MAX_METRIC = "rdct.system.cpu.utilization.window.max"
        const val PROCESS_USED_MEMORY_WINDOW_SAMPLES_METRIC = "rdct.process.memory.used.window.samples"
        const val SYSTEM_USED_MEMORY_WINDOW_SAMPLES_METRIC = "rdct.system.memory.used.window.samples"
        const val PROCESS_USED_MEMORY_WINDOW_AVERAGE_METRIC = "rdct.process.memory.used.window.avg"
        const val PROCESS_USED_MEMORY_WINDOW_MAX_METRIC = "rdct.process.memory.used.window.max"
        const val SYSTEM_USED_MEMORY_WINDOW_AVERAGE_METRIC = "rdct.system.memory.used.window.avg"
        const val SYSTEM_USED_MEMORY_WINDOW_MAX_METRIC = "rdct.system.memory.used.window.max"

        private const val METER_NAME = "com.jetbrains.otp.diagnostic"
    }
}
