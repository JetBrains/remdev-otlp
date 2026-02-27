package com.jetbrains.otp.exporter

import com.intellij.openapi.extensions.ExtensionPointName

interface OtlpConfigProvider {
    suspend fun createConfig(): OtlpConfig

    companion object {
        val EP_NAME = ExtensionPointName<OtlpConfigProvider>("com.jetbrains.otp.diagnostic.otlpConfigProvider")
    }
}