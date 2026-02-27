package com.jetbrains.otp.exporter.processor

import com.jetbrains.otp.exporter.OtlpConfig
import com.jetbrains.otp.settings.OtpDiagnosticSettings
import io.opentelemetry.sdk.trace.data.SpanData

object SpanScopeFilterProcessor : SpanProcessor {
    override fun process(spans: Collection<SpanData>, config: OtlpConfig): Collection<SpanData> {
        val settings = OtpDiagnosticSettings.getInstance()
        return spans.filter { span -> settings.isScopeEnabled(span.instrumentationScopeInfo.name) }
    }

    override fun getOrder(): Int = -2
}