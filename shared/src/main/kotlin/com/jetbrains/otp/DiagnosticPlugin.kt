package com.jetbrains.otp

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId

object DiagnosticPlugin {
    const val ID = "com.jetbrains.otp.diagnostic"

    fun version(): String? = runCatching {
        PluginManagerCore.getPlugin(PluginId.getId(ID))
            ?.version
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
