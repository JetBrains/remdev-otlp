package com.jetbrains.otp.freeze

import com.intellij.diagnostic.FreezeNotifier
import com.intellij.diagnostic.LogMessage
import com.intellij.diagnostic.ThreadDump
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationsManager
import com.intellij.openapi.diagnostic.Logger
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import java.nio.file.Path

@Suppress("UnstableApiUsage")
class OtpFreezeNotifier : FreezeNotifier {
    override fun notifyFreeze(
        event: LogMessage,
        currentDumps: Collection<ThreadDump>,
        reportDir: Path,
        durationMs: Long
    ) {
        Span.current().addEvent(
            "ui.frozen",
            Attributes.of(
                AttributeKey.stringKey("message"), event.throwable.message
            )
        )
    }
}