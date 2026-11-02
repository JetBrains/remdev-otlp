package com.jetbrains.otp.settings

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.otp.api.OtpDiagnosticSettingsApi

class FrontendPairedPluginStateEnforcementSyncActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        try {
            val disabled = OtpDiagnosticSettingsApi.getInstance().getDisablePairedPluginStateEnforcement()
            val registryValue = Registry.get(REGISTRY_KEY)
            if (registryValue.asBoolean() == disabled) {
                return
            }
            if (System.getProperty(REGISTRY_KEY) != null) {
                System.setProperty(REGISTRY_KEY, disabled.toString())
            }
            registryValue.setValue(disabled)
        } catch (e: Exception) {
            LOG.warn("Failed to synchronize paired plugin state enforcement from backend", e)
        }
    }

    companion object {
        private const val REGISTRY_KEY = "plugins.rd.plugin.sync.disable.paired.plugin.state.enforcement"
        private val LOG = Logger.getInstance(FrontendPairedPluginStateEnforcementSyncActivity::class.java)
    }
}
