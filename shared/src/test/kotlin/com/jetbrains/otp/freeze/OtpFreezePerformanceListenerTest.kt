package com.jetbrains.otp.freeze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpFreezePerformanceListenerTest {
    private val runLoop = StackTraceElement("java.awt.EventDispatchThread", "run", "EventDispatchThread.java", 92)
    private val slowAction = StackTraceElement("com.example.SlowAction", "perform", "SlowAction.kt", 10)
    private val otherAction = StackTraceElement("com.example.OtherAction", "perform", "OtherAction.kt", 20)

    @Test
    fun `reports only freezes above threshold with enough dumps and a common stack`() {
        assertTrue(OtpFreezePerformanceListener.shouldReport(11_000, 10, 2, listOf(runLoop)))
        assertFalse(OtpFreezePerformanceListener.shouldReport(10_999, 10, 2, listOf(runLoop)))
        assertFalse(OtpFreezePerformanceListener.shouldReport(11_000, 10, 1, listOf(runLoop)))
        assertFalse(OtpFreezePerformanceListener.shouldReport(11_000, 10, 2, emptyList()))
    }

    @Test
    fun `common stack keeps frames shared from the bottom`() {
        val common = OtpFreezePerformanceListener.commonStackSuffix(listOf(slowAction, runLoop), arrayOf(otherAction, runLoop))

        assertEquals(listOf(runLoop), common)
    }
}
