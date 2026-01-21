package com.jetbrains.otp.span

import com.intellij.util.messages.Topic


interface SessionSpanListener {
    fun sessionSpanInitialized(spanId: String, traceId: String)

    companion object {
        val TOPIC: Topic<SessionSpanListener> = Topic(SessionSpanListener::class.java)
    }
}