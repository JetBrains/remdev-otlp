package com.jetbrains.otp.exporter

import com.intellij.openapi.extensions.ExtensionPointName

interface AllowedMetricsProvider {
    fun getAllowedMetrics(): List<String>

    companion object {
        val EP_NAME = ExtensionPointName.create<AllowedMetricsProvider>("com.jetbrains.otp.diagnostic.allowedMetricsProvider")
    }
}