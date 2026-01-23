package com.jetbrains.otp.exporter.processor

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.sdk.common.InstrumentationScopeInfo
import io.opentelemetry.sdk.trace.data.EventData
import io.opentelemetry.sdk.trace.data.SpanData


class SpanDelegatingData(
    private val delegate: SpanData,
    private val newParent: SpanContext? = null,
    private val additionalEvents: List<EventData> = emptyList(),
    private val additionalAttributes: Attributes? = null
) : SpanData by delegate {

    override fun getTraceId(): String = newParent?.traceId ?: delegate.traceId
    override fun getSpanId(): String = delegate.spanId
    override fun getParentSpanContext(): SpanContext = newParent ?: delegate.parentSpanContext
    override fun getParentSpanId(): String = newParent?.spanId ?: delegate.parentSpanId
    override fun getInstrumentationScopeInfo(): InstrumentationScopeInfo = delegate.instrumentationScopeInfo

    override fun getEvents(): List<EventData> {
        return if (additionalEvents.isEmpty()) {
            delegate.events
        } else {
            delegate.events + additionalEvents
        }
    }

    override fun getAttributes(): Attributes {
        return if (additionalAttributes == null) {
            delegate.attributes
        } else {
            delegate.attributes.toBuilder()
                .putAll(additionalAttributes)
                .build()
        }
    }
}