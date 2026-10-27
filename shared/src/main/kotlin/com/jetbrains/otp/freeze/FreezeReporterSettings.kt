package com.jetbrains.otp.freeze

import com.intellij.openapi.util.registry.Registry

internal data class FreezeReporterSettings(
    val enabled: Boolean,
    val maxDumpDurationMs: Int,
    val durationThresholdSeconds: Int,
) {
    companion object {
        fun read(): FreezeReporterSettings = FreezeReporterSettings(
            enabled = Registry.`is`("freeze.reporter.enabled", false),
            maxDumpDurationMs = Registry.intValue("freeze.reporter.maxDumpDuration.ms", 40_000),
            durationThresholdSeconds = Registry.intValue("freeze.reporter.duration.threshold.seconds", 10).coerceAtLeast(0),
        )
    }
}
