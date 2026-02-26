package com.jetbrains.otp.exporter

import com.jetbrains.otp.exporter.processor.BufferingWrapperProcessor
import com.jetbrains.otp.exporter.processor.SpanProcessor
import com.jetbrains.otp.exporter.processor.SpanProcessorProvider
import com.jetbrains.otp.span.CommonSpanAttributes
import com.jetbrains.otp.span.CommonSpanAttributesState

class FrontendSpanProcessorProvider : SpanProcessorProvider {
    override fun getProcessors(): List<SpanProcessor> {
        return listOf(
            BufferingWrapperProcessor
        )
    }
}