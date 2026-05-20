@file:Suppress("UnstableApiUsage")

package com.jetbrains.otp

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.otp.settings.OtpVmOptions

class PluginInitializationService : ProjectActivity {
    private val optionsToSet = mapOf(
        "rdct.diagnostic.otlp" to "true",
        "idea.diagnostic.opentelemetry.otlp" to "true"
    )

    override suspend fun execute(project: Project) {
        try {
            optionsToSet.forEach { (key, value) ->
                OtpVmOptions.setProperty(key, value)
            }
        } catch (e: Exception) {
            LOG.warn("Failed to set VM options", e)
        }
    }
}


private val LOG = Logger.getInstance(PluginInitializationService::class.java)
