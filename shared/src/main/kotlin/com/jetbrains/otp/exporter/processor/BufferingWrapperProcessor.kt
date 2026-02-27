package com.jetbrains.otp.exporter.processor

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.otp.exporter.TelemetrySpanExporter
import com.jetbrains.otp.exporter.OtlpConfig
import io.opentelemetry.sdk.trace.data.SpanData
import java.util.concurrent.atomic.AtomicBoolean

object BufferingWrapperProcessor : SpanProcessor {
    private val bufferLock = Any()
    private val bufferedSpans = mutableListOf<SpanData>()
    private val tokenReceived = AtomicBoolean(false)

    override fun process(spans: Collection<SpanData>, config: OtlpConfig): Collection<SpanData> {
        if (tokenReceived.get()) {
            return spans
        }

        synchronized(bufferLock) {
            if (tokenReceived.get()) {
                return spans
            }

            bufferedSpans.addAll(spans)
            return emptyList()
        }
    }

    override fun getOrder(): Int = -1

    fun onCryptoReady() {
        val spans = synchronized(bufferLock) {
            if (tokenReceived.getAndSet(true)) {
                return
            }

            val spans = bufferedSpans.toList()
            bufferedSpans.clear()
            spans
        }

        if (spans.isNotEmpty()) {
            TelemetrySpanExporter.getInstance().sendSpans(spans)
        }
    }

    private val LOG = Logger.getInstance(BufferingWrapperProcessor::class.java)
}