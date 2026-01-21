package com.jetbrains.otp.exporter

import com.jetbrains.otp.span.SessionSpanListener

class ClientSessionSpanListener: SessionSpanListener {
    override fun sessionSpanInitialized(spanId: String, traceId: String) {
        TelemetrySpanExporter.getInstance().sessionSpanInitialized(spanId, traceId)
    }
}