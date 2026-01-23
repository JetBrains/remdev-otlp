package com.jetbrains.otp.exporter

import com.jetbrains.otp.exporter.processor.SpanProcessor
import com.jetbrains.otp.exporter.processor.SpanProcessorProvider

class BackendSpanProcessorProvider : SpanProcessorProvider {
    override fun getProcessors(): List<SpanProcessor> {
        return listOf(BackendAttributeProcessor())
    }
}