package com.jetbrains.otp.exporter

import com.intellij.platform.diagnostic.telemetry.impl.TelemetryReceivedListener
import io.opentelemetry.sdk.trace.data.SpanData

@Suppress("UnstableApiUsage")
class SpansReceivedListener: TelemetryReceivedListener {
    override fun sendSpans(spanData: Collection<SpanData>) {
        TelemetrySpanExporter.getInstance().sendSpans(spanData)
    }
}