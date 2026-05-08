package com.jetbrains.otp.exception.handler

import com.intellij.ide.plugins.PluginUtil
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord

object ErrorToSpanHandler : Handler() {
    override fun publish(record: LogRecord?) {
        if (record == null || record.level.intValue() < Level.SEVERE.intValue()) return

        val throwable = record.thrown ?: return
        val plugin = runCatching {
            PluginUtil.getInstance().findPluginId(throwable)?.idString
        }.getOrNull()
        runCatching {
            ExceptionSpanReporter.report(
                exception = throwable,
                context = mapOf(
                    "plugin" to plugin,
                    "log.logger" to record.loggerName,
                    "log.level" to record.level.name,
                    "log.message" to record.message,
                )
            )
        }
    }

    override fun flush() {}
    override fun close() {}
}
