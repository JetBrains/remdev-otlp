package com.jetbrains.otp.exporter

import io.opentelemetry.sdk.trace.data.SpanData
import java.util.concurrent.ConcurrentHashMap

/** Remembers spans exported ahead of the platform's span batch, so their later platform copy is not sent twice. */
internal class EarlyExportedSpans {
    // Only session root spans are exported early, so the set stays tiny.
    private val spanIds = ConcurrentHashMap.newKeySet<String>()

    /** Returns `false` when [span] was already exported early. */
    fun markExported(span: SpanData): Boolean = spanIds.add(span.spanContext.spanId)

    fun withoutExported(spans: Collection<SpanData>): Collection<SpanData> {
        if (spanIds.isEmpty()) return spans
        return spans.filterNot { it.spanContext.spanId in spanIds }
    }
}
