package com.jetbrains.otp.exporter.processor

import com.jetbrains.otp.exporter.OtlpConfig
import com.jetbrains.otp.settings.FrequentSpanFilterService
import io.opentelemetry.sdk.trace.data.SpanData

object FrequentSpanFilterProcessor : SpanProcessor {
    override fun process(spans: Collection<SpanData>, config: OtlpConfig): Collection<SpanData> {
        if (FrequentSpanFilterService.getInstance().isFrequentSpansEnabled()) return spans
        return spans.filterNot { span -> span.name in FREQUENT_SPAN_NAMES }
    }

    override fun getOrder(): Int = -3
}

private val FREQUENT_SPAN_NAMES = listOf(
    "run daemon",
    "show notification",
    "backend: getting items for the navigation bar",
    "backend: apply patch",
    "Getting files to scan",
    "Scanning gathered files",
    "rdct.reportHostStatus"
)