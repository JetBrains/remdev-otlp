package com.jetbrains.otp.exporter.processor

import com.jetbrains.otp.exporter.OtlpConfig
import com.jetbrains.otp.span.CommonSpanAttributesState
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.trace.data.SpanData

object CommonAttributesProcessor : SpanProcessor {
    override fun process(spans: Collection<SpanData>, config: OtlpConfig): Collection<SpanData> {
        val attributes = mergeAttributes(config)
        if (attributes.isEmpty) {
            return spans
        }

        return spans.map { span ->
            SpanDelegatingData(span, additionalAttributes = attributes)
        }
    }

    override fun getOrder(): Int = 1

    private fun mergeAttributes(config: OtlpConfig): Attributes {
        val runtimeAttributes = CommonSpanAttributesState.snapshot()
        if (runtimeAttributes.isEmpty && config.configuredSpanAttributes.isEmpty()) {
            return Attributes.empty()
        }

        val builder = Attributes.builder()
        config.configuredSpanAttributes.forEach { (key, value) ->
            builder.put(AttributeKey.stringKey(key), value)
        }
        builder.putAll(runtimeAttributes)
        return builder.build()
    }
}
