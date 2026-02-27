package com.jetbrains.otp.exporter.processor

import com.jetbrains.otp.exporter.OtlpConfig
import com.jetbrains.otp.settings.SpanFilterService
import io.opentelemetry.sdk.trace.data.SpanData

object SpanNameFilterProcessor : SpanProcessor {
    override fun process(spans: Collection<SpanData>, config: OtlpConfig): Collection<SpanData> {
        val filterService = SpanFilterService.getInstance()
        return spans.filter { span -> filterService.isSpanEnabled(span.name) }
    }

    override fun getOrder(): Int = -2
}
