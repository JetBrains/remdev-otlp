package com.jetbrains.otp.freeze

import com.intellij.diagnostic.LogMessage
import com.intellij.diagnostic.ThreadDump
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class FreezeReportingStrategyTest {
    private val spans = ArrayList<SpanData>()
    private lateinit var provider: SdkTracerProvider
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val autoReportEnabled = AtomicBoolean(false)
    private val checks = AtomicInteger()
    private val strategy = FreezeReportingStrategy(coroutineScope) {
        checks.incrementAndGet()
        autoReportEnabled.get()
    }
    private val reportDir = Path.of("freeze-test")
    private val edtFrame = StackTraceElement("com.example.SlowAction", "perform", "SlowAction.kt", 10)
    private val listenerStack = "com.intellij.diagnostic.Freeze\n\tat $edtFrame\n"
    private val dumps = List(2) { ThreadDump("dump $it", arrayOf(edtFrame), emptyArray()) }
    private val event = LogMessage(IllegalStateException("platform analyzed freeze"), null, emptyList())

    @Before
    fun setUp() {
        val exporter = object : SpanExporter {
            override fun export(batch: Collection<SpanData>): CompletableResultCode {
                spans.addAll(batch)
                return CompletableResultCode.ofSuccess()
            }

            override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()
            override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
        }
        provider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(exporter)).build()
        GlobalOpenTelemetry.resetForTest()
        GlobalOpenTelemetry.set(OpenTelemetrySdk.builder().setTracerProvider(provider).build())
    }

    @After
    fun tearDown() {
        coroutineScope.cancel()
        provider.close()
        GlobalOpenTelemetry.resetForTest()
    }

    @Test
    fun `disabled auto reporting keeps the notifier and its analyzed throwable`() = runBlocking {
        reportThroughBothStrategies().joinAll()

        assertSingleSpan()
        assertTrue(stack(spans.single()).contains("platform analyzed freeze"))
        assertEquals(1, checks.get())
    }

    @Test
    fun `auto reporting enabled by consent works without a notifier callback`() = runBlocking {
        autoReportEnabled.set(true)
        strategy.report(reportDir, fromListener = true, report = ::reportListener).join()

        assertListenerSpan()
    }

    @Test
    fun `enabled auto reporting emits only the listener span when both hooks arrive`() = runBlocking {
        autoReportEnabled.set(true)
        reportThroughBothStrategies().joinAll()

        assertListenerSpan()
        assertEquals(1, checks.get())
    }

    @Test
    fun `notifier arriving first still uses the selected listener strategy`() = runBlocking {
        autoReportEnabled.set(true)
        reportThroughBothStrategies(notifierFirst = true).joinAll()

        assertListenerSpan()
    }

    @Test
    fun `notifier arriving first reports once when auto reporting is disabled`() = runBlocking {
        reportThroughBothStrategies(notifierFirst = true).joinAll()

        assertSingleSpan()
        assertTrue(stack(spans.single()).contains("platform analyzed freeze"))
    }

    @Test
    fun `both hooks wait asynchronously for the same auto reporting check`() = runBlocking {
        val result = CompletableDeferred<Boolean>()
        val waitingStrategy = FreezeReportingStrategy(coroutineScope) {
            checks.incrementAndGet()
            result.await()
        }
        val jobs = reportThroughBothStrategies(strategy = waitingStrategy)
        assertTrue(spans.isEmpty())

        result.complete(true)
        jobs.joinAll()

        assertListenerSpan()
        assertEquals(1, checks.get())
    }

    @Test
    fun `settings changes between hooks do not produce a duplicate for the same freeze`() = runBlocking {
        autoReportEnabled.set(true)
        strategy.report(reportDir, fromListener = true, report = ::reportListener).join()
        autoReportEnabled.set(false)
        strategy.report(reportDir, fromListener = false, report = ::reportNotifier).join()

        assertListenerSpan()
        assertEquals(1, checks.get())
    }

    @Test
    fun `next freeze observes updated auto reporting settings`() = runBlocking {
        reportThroughBothStrategies().joinAll()
        autoReportEnabled.set(true)
        reportThroughBothStrategies(reportDir = Path.of("next-freeze")).joinAll()

        assertEquals(2, spans.size)
        assertTrue(stack(spans[0]).contains("platform analyzed freeze"))
        assertTrue(stack(spans[1]).startsWith("com.intellij.diagnostic.Freeze\n"))
        assertEquals(2, checks.get())
    }

    private fun reportThroughBothStrategies(
        strategy: FreezeReportingStrategy = this.strategy,
        reportDir: Path = this.reportDir,
        notifierFirst: Boolean = false,
    ): List<Job> {
        val sources = if (notifierFirst) listOf(false, true) else listOf(true, false)
        return sources.map { fromListener ->
            strategy.report(reportDir, fromListener, if (fromListener) ::reportListener else ::reportNotifier)
        }
    }

    private fun reportListener() = OtpFreezeReporter.reportFreeze(listenerStack, dumps, 11_000)

    private fun reportNotifier() = OtpFreezeReporter.reportFreeze(event.throwable.stackTraceToString(), dumps, 11_000)

    private fun stack(span: SpanData): String = span.attributes.get(AttributeKey.stringKey("stackTrace"))!!

    private fun assertSingleSpan() {
        assertEquals(1, spans.size)
        val span = spans.single()
        assertEquals("ui-thread-freeze", span.name)
        assertEquals(StatusCode.ERROR, span.status.statusCode)
        assertEquals(11_000_000_000L, span.endEpochNanos - span.startEpochNanos)
        assertEquals("dump 0", span.attributes.get(AttributeKey.stringKey("thread_dump_0")))
        assertEquals("dump 1", span.attributes.get(AttributeKey.stringKey("thread_dump_1")))
    }

    private fun assertListenerSpan() {
        assertSingleSpan()
        val stack = stack(spans.single())
        assertTrue(stack.startsWith("com.intellij.diagnostic.Freeze\n"))
        assertTrue(stack.contains(edtFrame.toString()))
    }
}
