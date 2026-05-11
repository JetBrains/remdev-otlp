package com.jetbrains.otp.span

import com.intellij.openapi.application.ApplicationInfo
import com.jetbrains.otp.DiagnosticPlugin

object CommonSpanAttributesInitializer {
    fun initialize(rdSide: String) {
        CommonSpanAttributesState.upsert(attributesFor(rdSide))
    }

    private fun attributesFor(rdSide: String): Map<String, String> = buildMap {
        put(CommonSpanAttributes.RD_SIDE, rdSide)
        DiagnosticPlugin.version()?.let { put(CommonSpanAttributes.PLUGIN_VERSION, it) }
        ApplicationInfo.getInstance()?.let { applicationInfo ->
            applicationInfo.fullVersion.takeIf { it.isNotBlank() }?.let {
                put(CommonSpanAttributes.IDEA_VERSION, it)
            }
            applicationInfo.build.asString().takeIf { it.isNotBlank() }?.let {
                put(CommonSpanAttributes.IDEA_BUILD, it)
            }
        }
    }
}
