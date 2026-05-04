package com.jetbrains.otp.exporter

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.lifetime
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.thinclient.diagnostics.ThinClientDiagnosticsService
import io.opentelemetry.api.metrics.ObservableLongGauge
import io.opentelemetry.api.metrics.ObservableLongMeasurement

class FrontendPingMetricReporter : ProjectActivity {
    override suspend fun execute(project: Project) {
        try {
            val diagnosticsService = ThinClientDiagnosticsService.getInstance(project)
            val uiPingGauge = registerPingGauge(
                metricName = UI_PING_METRIC,
                description = "UI-thread ping between frontend and backend",
                pingValue = { diagnosticsService.pingUiThread.value }
            )
            val networkPingGauge = registerPingGauge(
                metricName = NETWORK_PING_METRIC,
                description = "Direct protocol ping between frontend and backend",
                pingValue = { diagnosticsService.pingDirect.value }
            )

            project.lifetime.onTermination {
                uiPingGauge.close()
                networkPingGauge.close()
            }
        } catch (e: Exception) {
            LOG.warn("Failed to start frontend ping metrics reporting", e)
        }
    }

    private fun registerPingGauge(
        metricName: String,
        description: String,
        pingValue: () -> Int,
    ): ObservableLongGauge {
        return diagnosticMeter()
            .gaugeBuilder(metricName)
            .ofLongs()
            .setDescription(description)
            .setUnit("ms")
            .buildWithCallback { measurement ->
                recordPing(measurement, pingValue())
            }
    }

    private fun recordPing(measurement: ObservableLongMeasurement, pingMillis: Int) {
        if (pingMillis >= 0) {
            measurement.record(pingMillis.toLong())
        }
    }

    companion object {
        const val UI_PING_METRIC = "rdct.ping.ui"
        const val NETWORK_PING_METRIC = "rdct.ping.network"

        private val LOG = Logger.getInstance(FrontendPingMetricReporter::class.java)
    }
}
