package com.jetbrains.otp.exporter

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.otp.settings.OtpDiagnosticSettings
import com.sun.management.OperatingSystemMXBean
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.metrics.DoubleGauge
import io.opentelemetry.api.metrics.DoubleHistogram
import java.lang.management.ManagementFactory
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Service(Service.Level.APP)
class CpuUsageWindowMetricsReporter(
    private val coroutineScope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)
    private val lock = Any()
    private val reportLock = Any()
    private val operatingSystemMxBean = ManagementFactory.getOperatingSystemMXBean() as? OperatingSystemMXBean
    private val samples = ArrayDeque<CpuSample>()
    private val continuousReportingUntilMillis = AtomicLong(0L)
    private val lastReportedSampleEpochMillis = AtomicLong(0L)

    @Volatile
    private var rdSide: String = ""

    fun start(rdSide: String) {
        if (!started.compareAndSet(false, true)) return

        if (operatingSystemMxBean == null) {
            started.set(false)
            LOG.warn("CPU usage window metrics are disabled: OperatingSystemMXBean is unavailable")
            return
        }

        this.rdSide = rdSide
        try {
            coroutineScope.launch(Dispatchers.Default + CoroutineName(SAMPLER_COROUTINE_NAME)) {
                while (isActive) {
                    if (!isWindowMetricsReportingEnabled()) {
                        clearCollectedWindowState()
                        delay(SAMPLING_PERIOD_MILLIS)
                        continue
                    }

                    captureSample()
                    reportIfContinuousModeActive()
                    delay(SAMPLING_PERIOD_MILLIS)
                }
            }
        } catch (e: Exception) {
            started.set(false)
            LOG.warn("Failed to start CPU usage window sampling", e)
        }
    }

    fun getLast5Minutes(): CpuMetricsSnapshot {
        val now = System.currentTimeMillis()
        val snapshotSamples = synchronized(lock) {
            trimExpiredSamples(now)
            samples.toList()
        }
        return toSnapshot(snapshotSamples, now)
    }

    fun reportMetricsPrecisely() {
        if (!isWindowMetricsReportingEnabled()) {
            clearCollectedWindowState()
            return
        }

        val now = System.currentTimeMillis()
        scheduleContinuousReporting(now)
        synchronized(reportLock) {
            val snapshot = getLast5Minutes()
            reportSnapshot(snapshot, replayEntireWindow = true)
        }
    }

    private fun reportIfContinuousModeActive() {
        val now = System.currentTimeMillis()
        if (now > continuousReportingUntilMillis.get()) return
        if (!isWindowMetricsReportingEnabled()) {
            clearCollectedWindowState()
            return
        }
        synchronized(reportLock) {
            val snapshot = getLast5Minutes()
            reportSnapshot(snapshot, replayEntireWindow = false)
        }
    }

    private fun scheduleContinuousReporting(nowMillis: Long) {
        val newUntil = nowMillis + CONTINUOUS_REPORTING_DURATION_MILLIS
        while (true) {
            val current = continuousReportingUntilMillis.get()
            if (current >= newUntil) return
            if (continuousReportingUntilMillis.compareAndSet(current, newUntil)) return
        }
    }

    private fun reportSnapshot(snapshot: CpuMetricsSnapshot, replayEntireWindow: Boolean) {
        if (snapshot.samples.isEmpty()) return

        val attributes = buildCommonMetricAttributes(rdSide)
        val baselineEpoch = lastReportedSampleEpochMillis.get()
        val samplesToReport = if (replayEntireWindow) {
            snapshot.samples
        } else {
            snapshot.samples.filter { it.epochMillis > baselineEpoch }
        }

        var maxReportedEpoch = baselineEpoch
        samplesToReport.forEach { sample ->
            sample.processCpuLoad?.let { processWindowHistogram.record(it, attributes) }
            sample.systemCpuLoad?.let { systemWindowHistogram.record(it, attributes) }
            if (sample.epochMillis > maxReportedEpoch) {
                maxReportedEpoch = sample.epochMillis
            }
        }
        if (samplesToReport.isNotEmpty()) {
            lastReportedSampleEpochMillis.updateAndGet { current ->
                if (maxReportedEpoch > current) maxReportedEpoch else current
            }
        }

        snapshot.processAverage?.let { processWindowAverageGauge.set(it, attributes) }
        snapshot.processMax?.let { processWindowMaxGauge.set(it, attributes) }
        snapshot.systemAverage?.let { systemWindowAverageGauge.set(it, attributes) }
        snapshot.systemMax?.let { systemWindowMaxGauge.set(it, attributes) }
    }

    private fun captureSample() {
        val mxBean = operatingSystemMxBean ?: return
        val processCpuLoad = normalizeCpuLoad(mxBean.processCpuLoad)
        val systemCpuLoad = normalizeCpuLoad(mxBean.cpuLoad)

        if (processCpuLoad == null && systemCpuLoad == null) return

        val now = System.currentTimeMillis()
        synchronized(lock) {
            trimExpiredSamples(now)
            samples.addLast(
                CpuSample(
                    epochMillis = now,
                    processCpuLoad = processCpuLoad,
                    systemCpuLoad = systemCpuLoad,
                )
            )
        }
    }

    private fun normalizeCpuLoad(rawCpuLoad: Double): Double? {
        if (!rawCpuLoad.isFinite()) return null
        return rawCpuLoad.takeIf { it in 0.0..1.0 }
    }

    private fun trimExpiredSamples(nowMillis: Long) {
        val threshold = nowMillis - WINDOW_MILLIS
        while (samples.isNotEmpty() && samples.first().epochMillis < threshold) {
            samples.removeFirst()
        }
    }

    private fun clearCollectedWindowState() {
        var hadSamples = false
        synchronized(lock) {
            if (samples.isNotEmpty()) {
                samples.clear()
                hadSamples = true
            }
        }

        if (hadSamples || continuousReportingUntilMillis.get() != 0L || lastReportedSampleEpochMillis.get() != 0L) {
            continuousReportingUntilMillis.set(0L)
            lastReportedSampleEpochMillis.set(0L)
        }
    }

    private fun toSnapshot(samples: List<CpuSample>, nowMillis: Long): CpuMetricsSnapshot {
        val processValues = samples.mapNotNull { it.processCpuLoad }
        val systemValues = samples.mapNotNull { it.systemCpuLoad }

        return CpuMetricsSnapshot(
            fromEpochMillis = nowMillis - WINDOW_MILLIS,
            toEpochMillis = nowMillis,
            samplePeriodMillis = SAMPLING_PERIOD_MILLIS,
            samples = samples,
            processAverage = processValues.averageOrNullSafe(),
            processMax = processValues.maxOrNull(),
            systemAverage = systemValues.averageOrNullSafe(),
            systemMax = systemValues.maxOrNull(),
        )
    }

    private fun List<Double>.averageOrNullSafe(): Double? {
        if (isEmpty()) return null
        return average()
    }

    private fun isWindowMetricsReportingEnabled(): Boolean {
        return OtpDiagnosticSettings.getInstance().cpuWindowMetricsReportingEnabledEffective()
    }

    private val processWindowHistogram: DoubleHistogram by lazy {
        meter.histogramBuilder(PROCESS_CPU_WINDOW_SAMPLES_METRIC)
            .setDescription("Process CPU utilization samples from the last 5-minute on-demand report")
            .setUnit("1")
            .build()
    }

    private val systemWindowHistogram: DoubleHistogram by lazy {
        meter.histogramBuilder(SYSTEM_CPU_WINDOW_SAMPLES_METRIC)
            .setDescription("System CPU utilization samples from the last 5-minute on-demand report")
            .setUnit("1")
            .build()
    }

    private val processWindowAverageGauge: DoubleGauge by lazy {
        meter.gaugeBuilder(PROCESS_CPU_WINDOW_AVERAGE_METRIC)
            .setDescription("Average process CPU utilization for the last 5-minute on-demand report")
            .setUnit("1")
            .build()
    }

    private val processWindowMaxGauge: DoubleGauge by lazy {
        meter.gaugeBuilder(PROCESS_CPU_WINDOW_MAX_METRIC)
            .setDescription("Max process CPU utilization for the last 5-minute on-demand report")
            .setUnit("1")
            .build()
    }

    private val systemWindowAverageGauge: DoubleGauge by lazy {
        meter.gaugeBuilder(SYSTEM_CPU_WINDOW_AVERAGE_METRIC)
            .setDescription("Average system CPU utilization for the last 5-minute on-demand report")
            .setUnit("1")
            .build()
    }

    private val systemWindowMaxGauge: DoubleGauge by lazy {
        meter.gaugeBuilder(SYSTEM_CPU_WINDOW_MAX_METRIC)
            .setDescription("Max system CPU utilization for the last 5-minute on-demand report")
            .setUnit("1")
            .build()
    }

    private val meter by lazy {
        GlobalOpenTelemetry.get().getMeter(METER_NAME)
    }

    data class CpuSample(
        val epochMillis: Long,
        val processCpuLoad: Double?,
        val systemCpuLoad: Double?,
    )

    data class CpuMetricsSnapshot(
        val fromEpochMillis: Long,
        val toEpochMillis: Long,
        val samplePeriodMillis: Long,
        val samples: List<CpuSample>,
        val processAverage: Double?,
        val processMax: Double?,
        val systemAverage: Double?,
        val systemMax: Double?,
    )

    companion object {
        const val PROCESS_CPU_WINDOW_SAMPLES_METRIC = "rdct.process.cpu.utilization.window.samples"
        const val SYSTEM_CPU_WINDOW_SAMPLES_METRIC = "rdct.system.cpu.utilization.window.samples"
        const val PROCESS_CPU_WINDOW_AVERAGE_METRIC = "rdct.process.cpu.utilization.window.avg"
        const val PROCESS_CPU_WINDOW_MAX_METRIC = "rdct.process.cpu.utilization.window.max"
        const val SYSTEM_CPU_WINDOW_AVERAGE_METRIC = "rdct.system.cpu.utilization.window.avg"
        const val SYSTEM_CPU_WINDOW_MAX_METRIC = "rdct.system.cpu.utilization.window.max"

        private const val METER_NAME = "com.jetbrains.otp.diagnostic"
        private const val WINDOW_MILLIS = 5 * 60 * 1000L
        private const val SAMPLING_PERIOD_MILLIS = 5_000L
        private const val CONTINUOUS_REPORTING_DURATION_MILLIS = 5 * 60 * 1000L
        private const val SAMPLER_COROUTINE_NAME = "rdct-cpu-window-sampler"

        private val LOG = Logger.getInstance(CpuUsageWindowMetricsReporter::class.java)

        fun getInstance(): CpuUsageWindowMetricsReporter = service()
    }
}
