package com.jetbrains.otp.freeze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpFreezePerformanceListenerTest {
    private val runLoop = StackTraceElement("java.awt.EventDispatchThread", "run", "EventDispatchThread.java", 92)
    private val slowAction = StackTraceElement("com.example.SlowAction", "perform", "SlowAction.kt", 10)
    private val otherAction = StackTraceElement("com.example.OtherAction", "perform", "OtherAction.kt", 20)
    private val settings = FreezeReporterSettings(enabled = true, maxDumpDurationMs = 40_000, durationThresholdSeconds = 10)

    @Test
    fun `reports only freezes above threshold with enough dumps and a common stack`() {
        assertTrue(shouldReport())
        assertFalse(shouldReport(durationMs = 10_999))
        assertFalse(shouldReport(dumpCount = 1))
        assertFalse(shouldReport(stack = emptyList()))
        assertFalse(shouldReport(stack = null))
    }

    @Test
    fun `respects disabled reporting and a higher effective threshold`() {
        assertFalse(shouldReport(settings = settings.copy(enabled = false)))
        val remoteSettings = settings.copy(durationThresholdSeconds = 30)
        assertFalse(shouldReport(durationMs = 30_999, settings = remoteSettings))
        assertTrue(shouldReport(durationMs = 31_000, settings = remoteSettings))
    }

    @Test
    fun `respects disabled sampling without imposing a minimum positive duration`() {
        for (maxDuration in listOf(-1, 0)) {
            assertFalse("Maximum duration: $maxDuration", shouldReport(settings = settings.copy(maxDumpDurationMs = maxDuration)))
        }
        for (maxDuration in listOf(1, 100, 1_000, 1_001)) {
            assertTrue("Maximum duration: $maxDuration", shouldReport(settings = settings.copy(maxDumpDurationMs = maxDuration)))
        }
    }

    @Test
    fun `rejects intentional progress write actions`() {
        val progress = StackTraceElement(
            "com.intellij.openapi.application.impl.ApplicationImpl", "runEdtProgressWriteAction", "ApplicationImpl.java", 100,
        )
        assertFalse(shouldReport(stack = listOf(slowAction, progress, runLoop)))
        assertTrue(shouldReport(stack = listOf(slowAction, runLoop)))
    }

    @Test
    fun `common stack keeps frames shared from the bottom`() {
        val common = OtpFreezePerformanceListener.commonStackSuffix(listOf(slowAction, runLoop), arrayOf(otherAction, runLoop))

        assertEquals(listOf(runLoop), common)
    }

    @Test
    fun `keeps the slow method when only line numbers or module metadata change`() {
        val laterLine = StackTraceElement(
            "loader", "module", "1.0", slowAction.className, slowAction.methodName, slowAction.fileName, 11,
        )
        val first = listOf(slowAction, runLoop)
        assertEquals(first, OtpFreezePerformanceListener.commonStackSuffix(first, arrayOf(laterLine, runLoop)))
    }

    @Test
    fun `shrinks to the shorter stack and never regrows an empty common stack`() {
        assertEquals(listOf(runLoop), OtpFreezePerformanceListener.commonStackSuffix(listOf(slowAction, runLoop), arrayOf(runLoop)))
        assertEquals(emptyList<StackTraceElement>(), OtpFreezePerformanceListener.commonStackSuffix(emptyList(), arrayOf(runLoop)))
        assertEquals(emptyList<StackTraceElement>(), OtpFreezePerformanceListener.commonStackSuffix(listOf(runLoop), emptyArray()))
    }

    private fun shouldReport(
        durationMs: Long = 11_000,
        settings: FreezeReporterSettings = this.settings,
        dumpCount: Int = 2,
        stack: List<StackTraceElement>? = listOf(runLoop),
    ): Boolean = OtpFreezePerformanceListener.shouldReport(durationMs, settings, dumpCount, stack)
}
