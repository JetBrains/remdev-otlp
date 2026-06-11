package com.jetbrains.otp.exporter

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Service(Service.Level.APP)
class BackendPerformanceStatusReporter(
    private val coroutineScope: CoroutineScope,
) : AppLifecycleListener {
    private val started = AtomicBoolean(false)
    private val statusProvider = BackendPerformanceStatusProvider()
    private val spanTracker = PerformanceStatusSpanTracker(REPORTING_PERIOD_MILLIS)

    init {
        ApplicationManager.getApplication().messageBus.connect().subscribe(AppLifecycleListener.TOPIC, this)
    }

    fun start(rdSide: String) {
        if (!started.compareAndSet(false, true)) return

        coroutineScope.launch(Dispatchers.IO) {
            while (isActive) {
                reportOnce(rdSide)
                delay(REPORTING_PERIOD_MILLIS.milliseconds)
            }
        }
    }

    override fun appWillBeClosed(isRestart: Boolean) {
        spanTracker.closeAll(System.currentTimeMillis())
    }

    private fun reportOnce(rdSide: String) {
        try {
            spanTracker.report(
                alertStatuses = statusProvider.getAlertStatuses(),
                epochMillis = System.currentTimeMillis(),
                rdSide = rdSide,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LOG.warn("Failed to report host performance status spans", e)
        }
    }

    companion object {
        private const val REPORTING_PERIOD_MILLIS = 5_000L

        private val LOG = Logger.getInstance(BackendPerformanceStatusReporter::class.java)

        fun getInstance(): BackendPerformanceStatusReporter = service()
    }
}
