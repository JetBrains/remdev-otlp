package com.jetbrains.otp.exporter.processor

import com.jetbrains.otp.exporter.OtlpConfig
import com.jetbrains.otp.exporter.TelemetryAttributeRenamer
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.data.SpanData

object TelemetryAttributeRenamingProcessor : SpanProcessor {
    override fun process(spans: Collection<SpanData>, config: OtlpConfig): Collection<SpanData> {
        return spans.map { span -> TelemetryAttributeRenamingSpanData(span) }
    }

    override fun getOrder(): Int = Int.MAX_VALUE
}

private class TelemetryAttributeRenamingSpanData(
    private val delegate: SpanData
) : SpanData by delegate {
    override fun getAttributes(): Attributes {
        return TelemetryAttributeRenamer.rename(delegate.attributes)
    }

    override fun getResource(): Resource {
        return TelemetryAttributeRenamer.rename(delegate.resource)
    }
}