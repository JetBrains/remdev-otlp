package com.jetbrains.otp.exporter

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.otp.settings.OtpDiagnosticSettings
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Service(Service.Level.APP)
class FrequentPerformanceMetricsReporter(
    private val coroutineScope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)
    private val reportLock = Any()
    private val sampleCollector: PerformanceSampleCollector = JvmPerformanceSampleCollector()
    private val windowStorage = PerformanceWindowStorage(
        windowMillis = WINDOW_MILLIS,
        samplePeriodMillis = SAMPLING_PERIOD_MILLIS,
    )
    private val metricsEmitter = FrequentPerformanceMetricsEmitter()
    private val continuousReportingUntilMillis = AtomicLong(0L)
    private val lastReportedSampleEpochMillis = AtomicLong(0L)

    @Volatile
    private var rdSide: String = ""

    fun start(rdSide: String) {
        if (!started.compareAndSet(false, true)) return

        this.rdSide = rdSide
        try {
            coroutineScope.launch(Dispatchers.Default + CoroutineName(SAMPLER_COROUTINE_NAME)) {
                while (isActive) {
                    if (!isWindowMetricsReportingEnabled()) {
                        clearCollectedWindowState()
                        delay(SAMPLING_PERIOD_MILLIS)
                        continue
                    }

                    val now = System.currentTimeMillis()
                    sampleCollector.collectSample(now)?.let(windowStorage::addSample)
                    reportIfContinuousModeActive(now)
                    delay(SAMPLING_PERIOD_MILLIS)
                }
            }
        } catch (e: Exception) {
            started.set(false)
            LOG.warn("Failed to start frequent performance metrics sampling", e)
        }
    }

    fun reportMetricsPrecisely() {
        if (!isWindowMetricsReportingEnabled()) {
            clearCollectedWindowState()
            return
        }

        val now = System.currentTimeMillis()
        scheduleContinuousReporting(now)
        synchronized(reportLock) {
            reportSnapshot(windowStorage.snapshot(now), replayEntireWindow = true)
        }
    }

    private fun reportIfContinuousModeActive(nowMillis: Long) {
        if (nowMillis > continuousReportingUntilMillis.get()) return
        if (!isWindowMetricsReportingEnabled()) {
            clearCollectedWindowState()
            return
        }
        synchronized(reportLock) {
            reportSnapshot(windowStorage.snapshot(nowMillis), replayEntireWindow = false)
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

    private fun reportSnapshot(snapshot: PerformanceMetricsSnapshot, replayEntireWindow: Boolean) {
        if (snapshot.samples.isEmpty()) return

        val baselineEpoch = lastReportedSampleEpochMillis.get()
        val samplesToReport = if (replayEntireWindow) {
            snapshot.samples
        } else {
            snapshot.samples.filter { it.epochMillis > baselineEpoch }
        }

        var maxReportedEpoch = baselineEpoch
        samplesToReport.forEach { sample ->
            if (sample.epochMillis > maxReportedEpoch) {
                maxReportedEpoch = sample.epochMillis
            }
        }
        if (samplesToReport.isNotEmpty()) {
            lastReportedSampleEpochMillis.updateAndGet { current ->
                if (maxReportedEpoch > current) maxReportedEpoch else current
            }
        }

        metricsEmitter.emit(
            snapshot = snapshot,
            samplesToReport = samplesToReport,
            attributes = buildCommonMetricAttributes(rdSide)
        )
    }

    private fun clearCollectedWindowState() {
        windowStorage.clear()
        continuousReportingUntilMillis.set(0L)
        lastReportedSampleEpochMillis.set(0L)
    }

    private fun isWindowMetricsReportingEnabled(): Boolean {
        return OtpDiagnosticSettings.getInstance().frequentPerformanceMetricsReportingEnabledEffective()
    }

    companion object {
        private const val WINDOW_MILLIS = 5 * 60 * 1000L
        private const val SAMPLING_PERIOD_MILLIS = 5_000L
        private const val CONTINUOUS_REPORTING_DURATION_MILLIS = 5 * 60 * 1000L
        private const val SAMPLER_COROUTINE_NAME = "rdct-frequent-performance-sampler"

        private val LOG = Logger.getInstance(FrequentPerformanceMetricsReporter::class.java)

        fun getInstance(): FrequentPerformanceMetricsReporter = service()
    }
}
