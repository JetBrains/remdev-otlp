package com.jetbrains.otp.exporter

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
class TelemetryMetricExporter {
    val bufferingExporter = BufferingMetricExporter()

    fun initExporter(config: OtlpConfig) {
        try {
            val exporter = OtlpMetricExporterFactory.create(config)
            if (exporter != null) {
                bufferingExporter.setDelegate(exporter)
            }
        } catch (e: Exception) {
        }
    }

    companion object {
        fun getInstance(): TelemetryMetricExporter = service()
    }
}