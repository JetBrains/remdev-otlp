package com.jetbrains.otp.freeze

import com.intellij.diagnostic.ExceptionAutoReportUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.nio.file.Path

@Service(Service.Level.APP)
class FreezeReportingService(coroutineScope: CoroutineScope) {
    private val strategy = FreezeReportingStrategy(coroutineScope)

    internal fun report(reportDir: Path?, fromListener: Boolean, report: () -> Unit) {
        strategy.report(reportDir, fromListener, report)
    }

    companion object {
        fun getInstance(): FreezeReportingService = service()
    }
}

/** Shares one asynchronous auto-reporting decision between the two hooks for each freeze. */
internal class FreezeReportingStrategy(
    private val coroutineScope: CoroutineScope,
    private val isAutoReportEnabled: suspend () -> Boolean = { ExceptionAutoReportUtil.isAutoReportEnabled() },
) {
    private val decisions = LinkedHashMap<Path, Deferred<Boolean>>()

    fun report(reportDir: Path?, fromListener: Boolean, report: () -> Unit): Job {
        val decision = synchronized(decisions) {
            val existing = reportDir?.let { decisions[it] }
            existing ?: coroutineScope.async(Dispatchers.Default, start = CoroutineStart.LAZY) {
                isAutoReportEnabled()
            }.also {
                if (reportDir != null) {
                    decisions[reportDir] = it
                    // Notifier callbacks can arrive later, or be skipped entirely by the platform.
                    if (decisions.size > 100) decisions.remove(decisions.keys.first())
                }
            }
        }

        return coroutineScope.launch(Dispatchers.Default) {
            if (decision.await() == fromListener) report()
        }
    }
}
