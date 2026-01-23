package com.jetbrains.otp.exporter

import com.jetbrains.otp.exporter.processor.SpanProcessor
import com.jetbrains.otp.exporter.processor.SpanProcessorProvider

class FrontendSpanProcessorProvider : SpanProcessorProvider {
    override fun getProcessors(): List<SpanProcessor> {
        return listOf(FrontendAttributeProcessor())
    }
}