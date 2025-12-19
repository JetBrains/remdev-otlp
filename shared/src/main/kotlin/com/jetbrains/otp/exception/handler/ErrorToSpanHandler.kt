package com.jetbrains.otp.exception.handler

import com.intellij.ide.plugins.PluginUtil
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import java.util.logging.Handler
import java.util.logging.LogRecord

object ErrorToSpanHandler : Handler() {
    override fun publish(record: LogRecord?) {
        val throwable = record?.thrown ?: return
        val plugin = PluginUtil.getInstance().findPluginId(throwable)?.idString
        val attributes: Attributes = plugin?.let { Attributes.of(AttributeKey.stringKey("plugin"), it) }
            ?: Attributes.empty()
        Span.current().recordException(throwable, attributes)
    }

    override fun flush() {}
    override fun close() {}
}