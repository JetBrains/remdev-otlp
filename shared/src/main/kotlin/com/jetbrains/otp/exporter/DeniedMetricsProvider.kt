package com.jetbrains.otp.exporter

import com.intellij.openapi.extensions.ExtensionPointName

interface DeniedMetricsProvider {
    fun getDeniedMetrics(): List<String>

    companion object {
        val EP_NAME = ExtensionPointName.create<DeniedMetricsProvider>("com.jetbrains.otp.diagnostic.deniedMetricsProvider")
    }
}
