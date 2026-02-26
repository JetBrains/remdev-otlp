package com.jetbrains.otp.exporter.processor

import com.jetbrains.otp.span.CommonSpanAttributesState
import io.opentelemetry.sdk.trace.data.SpanData

object CommonAttributesProcessor : SpanProcessor {
    override fun process(spans: Collection<SpanData>): Collection<SpanData> {
        val attributes = CommonSpanAttributesState.snapshot()
        if (attributes.isEmpty) {
            return spans
        }

        return spans.map { span ->
            SpanDelegatingData(span, additionalAttributes = attributes)
        }
    }

    override fun getOrder(): Int = 1
}
