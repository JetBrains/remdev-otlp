package com.jetbrains.otp.exporter.processor

import com.jetbrains.otp.exporter.OtlpConfig
import io.opentelemetry.sdk.trace.data.SpanData

interface SpanProcessor {
    fun process(spans: Collection<SpanData>, config: OtlpConfig): Collection<SpanData>

    fun getOrder(): Int
}