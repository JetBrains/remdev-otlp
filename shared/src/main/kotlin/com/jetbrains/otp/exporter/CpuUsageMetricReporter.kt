package com.jetbrains.otp.exporter

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.platform.diagnostic.telemetry.PlatformMetrics
import com.intellij.platform.diagnostic.telemetry.TelemetryManager
import com.jetbrains.otp.span.CommonSpanAttributes
import com.jetbrains.otp.span.CommonSpanAttributesState
import com.sun.management.OperatingSystemMXBean
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.ObservableDoubleGauge
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.APP)
class CpuUsageMetricReporter {
    private val started = AtomicBoolean(false)
    private val operatingSystemMxBean = ManagementFactory.getOperatingSystemMXBean() as? OperatingSystemMXBean

    private var processCpuGauge: ObservableDoubleGauge? = null
    private var systemCpuGauge: ObservableDoubleGauge? = null

    fun start(@Suppress("UNUSED_PARAMETER") rdSide: String) {
        if (!started.compareAndSet(false, true)) return

        val mxBean = operatingSystemMxBean
        if (mxBean == null) {
            started.set(false)
            LOG.warn("CPU usage metrics reporting is disabled: OperatingSystemMXBean is unavailable")
            return
        }

        try {
            val meter = TelemetryManager.getMeter(PlatformMetrics)

            processCpuGauge = meter.gaugeBuilder(PROCESS_CPU_UTILIZATION_METRIC)
                .setDescription("Recent CPU utilization of the current process")
                .setUnit("1")
                .buildWithCallback { measurement ->
                    recordCpuLoad(measurement, mxBean.processCpuLoad)
                }

            systemCpuGauge = meter.gaugeBuilder(SYSTEM_CPU_UTILIZATION_METRIC)
                .setDescription("Recent CPU utilization of the host system")
                .setUnit("1")
                .buildWithCallback { measurement ->
                    recordCpuLoad(measurement, mxBean.cpuLoad)
                }
        } catch (e: Exception) {
            started.set(false)
            LOG.warn("Failed to start CPU usage metrics reporting", e)
        }
    }

    private fun recordCpuLoad(
        measurement: ObservableDoubleMeasurement,
        rawCpuLoad: Double
    ) {
        val cpuLoad = normalizeCpuLoad(rawCpuLoad) ?: return
        measurement.record(cpuLoad)
    }

    private fun normalizeCpuLoad(rawCpuLoad: Double): Double? {
        if (!rawCpuLoad.isFinite()) return null
        return rawCpuLoad.takeIf { it in 0.0..1.0 }
    }

    companion object {
        const val PROCESS_CPU_UTILIZATION_METRIC = "rdct.process.cpu.utilization"
        const val SYSTEM_CPU_UTILIZATION_METRIC = "rdct.system.cpu.utilization"

        private val LOG = Logger.getInstance(CpuUsageMetricReporter::class.java)

        fun getInstance(): CpuUsageMetricReporter = service()
    }
}

internal fun buildCommonMetricAttributes(@Suppress("UNUSED_PARAMETER") rdSide: String): Attributes {
    val commonAttributes = CommonSpanAttributesState.snapshotMap()
    if (commonAttributes.isEmpty() && rdSide.isBlank()) return Attributes.empty()

    val builder = Attributes.builder()
    commonAttributes.forEach { (key, value) ->
        builder.put(AttributeKey.stringKey(key), value)
    }
    if (rdSide.isNotBlank()) {
        builder.put(AttributeKey.stringKey(CommonSpanAttributes.RD_SIDE), rdSide)
    }
    return builder.build()
}
