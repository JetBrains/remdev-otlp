package com.jetbrains.otp.settings

import com.intellij.diagnostic.VMOptions
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.otp.api.OtpDiagnosticSettingsApi

class FrontendXmxSyncActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        try {
            val targetXmxMb = OtpDiagnosticSettingsApi.getInstance().getFrontendXmxMb() ?: return
            val configuredXmxMb = VMOptions.readOption(VMOptions.MemoryKind.HEAP, false)
            if (configuredXmxMb == targetXmxMb) {
                return
            }

            OtpVmOptions.setHeapSizeMb(targetXmxMb)
        } catch (e: Exception) {
            LOG.warn("Failed to synchronize frontend Xmx from backend", e)
        }
    }

    companion object {
        private val LOG = Logger.getInstance(FrontendXmxSyncActivity::class.java)
    }
}
