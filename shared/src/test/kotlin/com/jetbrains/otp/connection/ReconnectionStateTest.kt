package com.jetbrains.otp.connection

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class ReconnectionStateTest {
    private var nowNanos = 0L
    private val telemetry = FakeReconnectionTelemetry()
    private val limiter = AdaptiveCooldownReconnectionSpanLimiter { nowNanos }

    @Test
    fun `creates and ends the first reconnect span`() {
        val state = state()

        state.disconnected(mapOf("drop" to "initial"))
        state.connected(mapOf("connected" to "true"))

        assertEquals(1, telemetry.reportMetricsCount)
        assertEquals(1, telemetry.startedSpans.size)
        assertEquals("test-reconnect", telemetry.startedSpans.single().spanName)
        assertEquals(mapOf("drop" to "initial"), telemetry.startedSpans.single().startContext)
        assertEquals(mapOf("connected" to "true"), telemetry.startedSpans.single().connectedContexts.single())
        assertEquals(1, telemetry.startedSpans.single().endCount)
    }

    @Test
    fun `suppresses reconnect attempts inside base cooldown and emits after cooldown expires`() {
        val state = state(baseCooldownMillis = 1_000L)

        state.disconnected()
        state.connected()
        nowNanos = TimeUnit.MILLISECONDS.toNanos(500L)
        state.disconnected()
        state.connected()

        assertEquals(1, telemetry.reportMetricsCount)
        assertEquals(1, telemetry.startedSpans.size)

        nowNanos = TimeUnit.MILLISECONDS.toNanos(1_500L)
        state.disconnected()

        assertEquals(2, telemetry.reportMetricsCount)
        assertEquals(2, telemetry.startedSpans.size)
        assertEquals(1L, telemetry.startedSpans.last().throttledConnectionDropCount)
    }

    @Test
    fun `repeated cooldown violations grow the next cooldown`() {
        val state = state(
            baseCooldownMillis = 1_000L,
            maxCooldownMillis = 8_000L,
        )

        state.disconnected()
        state.connected()
        nowNanos = TimeUnit.MILLISECONDS.toNanos(500L)
        state.disconnected()
        state.connected()
        nowNanos = TimeUnit.MILLISECONDS.toNanos(1_000L)
        state.disconnected()
        state.connected()
        nowNanos = TimeUnit.MILLISECONDS.toNanos(2_000L)
        state.disconnected()
        state.connected()

        assertEquals(1, telemetry.reportMetricsCount)
        assertEquals(1, telemetry.startedSpans.size)

        nowNanos = TimeUnit.MILLISECONDS.toNanos(6_000L)
        state.disconnected()

        assertEquals(2, telemetry.reportMetricsCount)
        assertEquals(2, telemetry.startedSpans.size)
        assertEquals(3L, telemetry.startedSpans.last().throttledConnectionDropCount)
    }

    @Test
    fun `growing cooldown is capped`() {
        val state = state(
            baseCooldownMillis = 1_000L,
            maxCooldownMillis = 2_000L,
        )

        state.disconnected()
        state.connected()
        nowNanos = TimeUnit.MILLISECONDS.toNanos(500L)
        state.disconnected()
        state.connected()
        nowNanos = TimeUnit.MILLISECONDS.toNanos(1_000L)
        state.disconnected()
        state.connected()
        nowNanos = TimeUnit.MILLISECONDS.toNanos(2_500L)
        state.disconnected()
        state.connected()

        assertEquals(1, telemetry.reportMetricsCount)
        assertEquals(1, telemetry.startedSpans.size)

        nowNanos = TimeUnit.MILLISECONDS.toNanos(4_500L)
        state.disconnected()

        assertEquals(2, telemetry.reportMetricsCount)
        assertEquals(2, telemetry.startedSpans.size)
        assertEquals(3L, telemetry.startedSpans.last().throttledConnectionDropCount)
    }

    @Test
    fun `cooldown level resets after a quiet period`() {
        val state = state(
            baseCooldownMillis = 1_000L,
            maxCooldownMillis = 8_000L,
            cooldownResetMillis = 5_000L,
        )

        state.disconnected()
        state.connected()
        nowNanos = TimeUnit.MILLISECONDS.toNanos(500L)
        state.disconnected()
        state.connected()
        nowNanos = TimeUnit.MILLISECONDS.toNanos(1_000L)
        state.disconnected()
        state.connected()

        nowNanos = TimeUnit.MILLISECONDS.toNanos(7_000L)
        state.disconnected()
        state.connected()

        assertEquals(2, telemetry.reportMetricsCount)
        assertEquals(2, telemetry.startedSpans.size)
        assertEquals(2L, telemetry.startedSpans.last().throttledConnectionDropCount)

        nowNanos = TimeUnit.MILLISECONDS.toNanos(7_500L)
        state.disconnected()
        state.connected()
        nowNanos = TimeUnit.MILLISECONDS.toNanos(8_500L)
        state.disconnected()

        assertEquals(3, telemetry.reportMetricsCount)
        assertEquals(3, telemetry.startedSpans.size)
        assertEquals(1L, telemetry.startedSpans.last().throttledConnectionDropCount)
    }

    @Test
    fun `zero base cooldown disables reconnect throttling`() {
        val state = state(
            baseCooldownMillis = 0L,
            maxCooldownMillis = 0L,
            cooldownResetMillis = 0L,
        )

        state.disconnected()
        state.connected()
        state.disconnected()
        state.connected()

        assertEquals(2, telemetry.reportMetricsCount)
        assertEquals(2, telemetry.startedSpans.size)
        assertEquals(0L, telemetry.startedSpans.last().throttledConnectionDropCount)
    }

    @Test
    fun `shares reconnect span cooldown across state instances`() {
        val firstState = state(baseCooldownMillis = 1_000L)
        val secondState = state(baseCooldownMillis = 1_000L)

        firstState.disconnected()
        nowNanos = TimeUnit.MILLISECONDS.toNanos(1_500L)
        secondState.disconnected()

        assertEquals(1, telemetry.reportMetricsCount)
        assertEquals(1, telemetry.startedSpans.size)

        nowNanos = TimeUnit.MILLISECONDS.toNanos(2_000L)
        firstState.connected()
        nowNanos = TimeUnit.MILLISECONDS.toNanos(3_000L)
        secondState.connected()
        secondState.disconnected()

        assertEquals(2, telemetry.reportMetricsCount)
        assertEquals(2, telemetry.startedSpans.size)
        assertEquals(1L, telemetry.startedSpans.last().throttledConnectionDropCount)
    }

    @Test
    fun `duplicate disconnected events while already disconnected are ignored`() {
        val state = state()

        state.disconnected()
        state.disconnected()
        state.connected()

        assertEquals(1, telemetry.reportMetricsCount)
        assertEquals(1, telemetry.startedSpans.size)
        assertEquals(1, telemetry.startedSpans.single().endCount)
    }

    private fun state(
        baseCooldownMillis: Long = 1_000L,
        maxCooldownMillis: Long = 8_000L,
        cooldownResetMillis: Long = 60_000L,
    ): ReconnectionState {
        return ReconnectionState(
            config = ReconnectionSpanConfig(
                spanName = "test-reconnect",
                baseCooldownMillis = baseCooldownMillis,
                maxCooldownMillis = maxCooldownMillis,
                cooldownResetMillis = cooldownResetMillis,
            ),
            telemetry = telemetry,
            limiter = limiter,
        )
    }

    private class FakeReconnectionTelemetry : ReconnectionTelemetry {
        var reportMetricsCount = 0
        val startedSpans = mutableListOf<FakeReconnectionSpan>()

        override fun reportMetricsAfterConnectionDrop() {
            reportMetricsCount++
        }

        override fun startSpan(
            spanName: String,
            context: Map<String, String>,
            throttledConnectionDropCount: Long,
        ): ReconnectionSpan {
            return FakeReconnectionSpan(
                spanName = spanName,
                startContext = context,
                throttledConnectionDropCount = throttledConnectionDropCount,
            ).also(startedSpans::add)
        }
    }

    private class FakeReconnectionSpan(
        val spanName: String,
        val startContext: Map<String, String>,
        val throttledConnectionDropCount: Long,
    ) : ReconnectionSpan {
        val connectedContexts = mutableListOf<Map<String, String>>()
        var endCount = 0

        override fun setAttributes(context: Map<String, String>) {
            connectedContexts += context
        }

        override fun end() {
            endCount++
        }
    }
}
