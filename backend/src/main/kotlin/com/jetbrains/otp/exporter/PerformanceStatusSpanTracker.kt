package com.jetbrains.otp.exporter

import com.jetbrains.otp.span.CommonSpanAttributes
import com.jetbrains.otp.span.CommonSpanAttributesState
import com.jetbrains.otp.span.DefaultRootSpanService
import com.jetbrains.rd.platform.codeWithMe.unattendedHost.metrics.MetricsStatus
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import java.time.Instant

internal class PerformanceStatusSpanTracker(
    private val minimumSpanDurationMillis: Long,
) {
    private val lock = Any()
    private val trackedSpans = mutableMapOf<PerformanceMetric, TrackedPerformanceStatusSpan>()

    fun report(alertStatuses: List<PerformanceStatus>, epochMillis: Long, rdSide: String) {
        val now = Instant.ofEpochMilli(epochMillis)

        synchronized(lock) {
            closeMissingSpans(alertStatuses.mapTo(mutableSetOf()) { it.metric }, now)
            alertStatuses.forEach { status -> updateSpan(status, rdSide, now) }
        }
    }

    fun closeAll(epochMillis: Long) {
        synchronized(lock) {
            closeAll(Instant.ofEpochMilli(epochMillis))
        }
    }

    private fun updateSpan(status: PerformanceStatus, rdSide: String, now: Instant) {
        if (status.status == MetricsStatus.NORMAL) {
            closeTrackedSpan(status.metric, now)
            return
        }

        val isDanger = isDanger(status.status)
        val trackedSpan = trackedSpans[status.metric]

        when (trackedSpan) {
            null -> startPendingSpan(status, isDanger, now)
            is PendingPerformanceStatusSpan -> updatePendingSpan(status, trackedSpan, isDanger, rdSide, now)
            is StartedPerformanceStatusSpan -> updateStartedSpan(status, trackedSpan, isDanger, now)
        }
    }

    private fun updatePendingSpan(
        status: PerformanceStatus,
        pendingSpan: PendingPerformanceStatusSpan,
        isDanger: Boolean,
        rdSide: String,
        now: Instant,
    ) {
        if (pendingSpan.isDanger != isDanger) {
            startPendingSpan(status, isDanger, now)
            return
        }

        if (shouldStartSpan(pendingSpan.startTime, now)) {
            trackedSpans[status.metric] = StartedPerformanceStatusSpan(
                isDanger = isDanger,
                span = startSpan(status, isDanger, rdSide, pendingSpan.startTime),
            )
        }
    }

    private fun updateStartedSpan(
        status: PerformanceStatus,
        startedSpan: StartedPerformanceStatusSpan,
        isDanger: Boolean,
        now: Instant,
    ) {
        if (startedSpan.isDanger != isDanger) {
            startedSpan.span.end(now)
            startPendingSpan(status, isDanger, now)
            return
        }

        setDynamicAttributes(startedSpan.span, status, isDanger)
    }

    private fun startPendingSpan(
        status: PerformanceStatus,
        isDanger: Boolean,
        now: Instant,
    ) {
        trackedSpans[status.metric] = PendingPerformanceStatusSpan(
            isDanger = isDanger,
            startTime = now,
        )
    }

    private fun shouldStartSpan(startTime: Instant, now: Instant): Boolean {
        return now.toEpochMilli() - startTime.toEpochMilli() >= minimumSpanDurationMillis
    }

    private fun closeMissingSpans(currentMetrics: Set<PerformanceMetric>, now: Instant) {
        val missingMetrics = trackedSpans.keys - currentMetrics
        missingMetrics.forEach { metric -> closeTrackedSpan(metric, now) }
    }

    private fun closeTrackedSpan(metric: PerformanceMetric, now: Instant) {
        val trackedSpan = trackedSpans.remove(metric) ?: return
        if (trackedSpan is StartedPerformanceStatusSpan) {
            trackedSpan.span.end(now)
        }
    }

    private fun closeAll(now: Instant) {
        trackedSpans.values.forEach { trackedSpan ->
            if (trackedSpan is StartedPerformanceStatusSpan) {
                trackedSpan.span.end(now)
            }
        }
        trackedSpans.clear()
    }

    private fun startSpan(
        status: PerformanceStatus,
        isDanger: Boolean,
        rdSide: String,
        startTime: Instant,
    ): Span {
        val span = DefaultRootSpanService.TRACER.spanBuilder(status.metric.spanName(isDanger))
            .setParent(Context.current().with(DefaultRootSpanService.currentSpan()))
            .setStartTimestamp(startTime)
            .setAllAttributes(createAttributes(status, isDanger, rdSide))
            .startSpan()

        try {
            if (isDanger) {
                span.setStatus(StatusCode.ERROR, status.metric.alertText(isDanger))
            }
        } catch (error: Throwable) {
            span.end(startTime)
            throw error
        }

        return span
    }

    private fun createAttributes(
        status: PerformanceStatus,
        isDanger: Boolean,
        rdSide: String,
    ): Attributes {
        val builder = Attributes.builder()
            .putAll(commonAttributes(rdSide))
            .put(METRIC_ATTRIBUTE, status.metric.attributeValue)
            .put(STATUS_ATTRIBUTE, status.status.name.lowercase())
            .put(TEXT_ATTRIBUTE, status.metric.alertText(isDanger))
            .put(UNIT_ATTRIBUTE, status.unit)

        status.message?.let {
            builder.put(MESSAGE_ATTRIBUTE, it)
        }
        status.value?.let {
            builder.put(VALUE_ATTRIBUTE, it)
        }

        return builder.build()
    }

    private fun commonAttributes(rdSide: String): Attributes {
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

    private fun setDynamicAttributes(
        span: Span,
        status: PerformanceStatus,
        isDanger: Boolean,
    ) {
        span.setAttribute(STATUS_ATTRIBUTE, status.status.name.lowercase())
        span.setAttribute(TEXT_ATTRIBUTE, status.metric.alertText(isDanger))
        status.message?.let {
            span.setAttribute(MESSAGE_ATTRIBUTE, it)
        }
        status.value?.let {
            span.setAttribute(VALUE_ATTRIBUTE, it)
        }
    }

    private sealed interface TrackedPerformanceStatusSpan {
        val isDanger: Boolean
    }

    private data class PendingPerformanceStatusSpan(
        override val isDanger: Boolean,
        val startTime: Instant,
    ) : TrackedPerformanceStatusSpan

    private data class StartedPerformanceStatusSpan(
        override val isDanger: Boolean,
        val span: Span,
    ) : TrackedPerformanceStatusSpan

    private companion object {
        private val METRIC_ATTRIBUTE = AttributeKey.stringKey("rdct.performance.metric")
        private val STATUS_ATTRIBUTE = AttributeKey.stringKey("rdct.performance.status")
        private val TEXT_ATTRIBUTE = AttributeKey.stringKey("rdct.performance.text")
        private val UNIT_ATTRIBUTE = AttributeKey.stringKey("rdct.performance.unit")
        private val MESSAGE_ATTRIBUTE = AttributeKey.stringKey("rdct.performance.message")
        private val VALUE_ATTRIBUTE = AttributeKey.doubleKey("rdct.performance.value")
    }
}

internal data class PerformanceStatus(
    val metric: PerformanceMetric,
    val status: MetricsStatus,
    val value: Double?,
    val unit: String,
    val message: String?,
)

private fun isDanger(status: MetricsStatus): Boolean {
    return when (status) {
        MetricsStatus.WARNING,
        MetricsStatus.WARNING_RESOURCES_LOW,
        MetricsStatus.NORMAL -> false
        MetricsStatus.DANGER,
        MetricsStatus.DANGER_RESOURCES_CRITICAL -> true
    }
}

internal enum class PerformanceMetric(
    val attributeValue: String,
    private val warningSpanName: String,
    private val dangerSpanName: String,
    private val warningText: String,
    private val dangerText: String,
) {
    SYSTEM_MEMORY(
        attributeValue = "system_memory",
        warningSpanName = "rdct.host.performance.high_system_memory_usage",
        dangerSpanName = "rdct.host.performance.critical_system_memory_usage",
        warningText = "high system memory usage",
        dangerText = "critical system memory usage",
    ),
    JVM_MEMORY(
        attributeValue = "jvm_memory",
        warningSpanName = "rdct.host.performance.high_jvm_memory_usage",
        dangerSpanName = "rdct.host.performance.critical_jvm_memory_usage",
        warningText = "high JVM memory usage",
        dangerText = "critical JVM memory usage",
    ),
    SYSTEM_CPU(
        attributeValue = "system_cpu",
        warningSpanName = "rdct.host.performance.high_system_cpu_load",
        dangerSpanName = "rdct.host.performance.critical_system_cpu_load",
        warningText = "high system CPU load",
        dangerText = "critical system CPU load",
    ),
    DISK(
        attributeValue = "disk",
        warningSpanName = "rdct.host.performance.high_disk_usage",
        dangerSpanName = "rdct.host.performance.critical_disk_usage",
        warningText = "high disk usage",
        dangerText = "critical disk usage",
    ),
    LOAD_AVERAGE(
        attributeValue = "load_average_1m",
        warningSpanName = "rdct.host.performance.high_system_load_average_1m",
        dangerSpanName = "rdct.host.performance.critical_system_load_average_1m",
        warningText = "high system load average 1m",
        dangerText = "critical system load average 1m",
    );

    fun spanName(isDanger: Boolean): String {
        return if (isDanger) dangerSpanName else warningSpanName
    }

    fun alertText(isDanger: Boolean): String {
        return if (isDanger) dangerText else warningText
    }
}
