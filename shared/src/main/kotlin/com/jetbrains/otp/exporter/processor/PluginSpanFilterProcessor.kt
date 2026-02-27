package com.jetbrains.otp.exporter.processor

import com.jetbrains.otp.exporter.OtlpConfig
import com.jetbrains.otp.span.DefaultRootSpanService
import io.opentelemetry.sdk.trace.data.SpanData

object PluginSpanFilterProcessor : SpanProcessor {
    override fun process(spans: Collection<SpanData>, config: OtlpConfig): Collection<SpanData> {
        if (!config.isPluginSpanFilterEnabled) return spans

        return spans.filter { span ->
            span.instrumentationScopeInfo.name == DefaultRootSpanService.DIAGNOSTIC_TRACER_NAME
        }
    }

    override fun getOrder(): Int = -4
}
