package com.jetbrains.otp.span

import com.jetbrains.otp.DiagnosticPlugin

object CommonSpanAttributesInitializer {
    fun initialize(rdSide: String) {
        CommonSpanAttributesState.upsert(attributesFor(rdSide))
    }

    private fun attributesFor(rdSide: String): Map<String, String> = buildMap {
        put(CommonSpanAttributes.RD_SIDE, rdSide)
        DiagnosticPlugin.version()?.let { put(CommonSpanAttributes.PLUGIN_VERSION, it) }
    }
}
