package com.jetbrains.otp.exporter

import com.jetbrains.otp.exporter.processor.SpanDelegatingData
import com.jetbrains.otp.exporter.processor.SpanProcessor
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.trace.data.SpanData

class FrontendAttributeProcessor : SpanProcessor {
    override fun process(spans: Collection<SpanData>): Collection<SpanData> {
        val attributes = Attributes.builder()
            .put(AttributeKey.stringKey("rd-side"), "frontend")
            .build()

        return spans.map { span ->
            SpanDelegatingData(span, additionalAttributes = attributes)
        }
    }

    override fun getOrder(): Int = 100
}