package com.jetbrains.otp.freeze

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Exports freeze reports off the platform's freeze-listener chain, which invokes listeners sequentially. */
@Service(Service.Level.APP)
class FreezeReportingService(private val coroutineScope: CoroutineScope) {
    internal fun report(report: () -> Unit) {
        coroutineScope.launch(Dispatchers.Default) { report() }
    }

    companion object {
        fun getInstance(): FreezeReportingService = service()
    }
}
