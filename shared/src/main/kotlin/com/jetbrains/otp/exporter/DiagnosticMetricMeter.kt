package com.jetbrains.otp.exporter

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.metrics.Meter

const val DIAGNOSTIC_METER_NAME = "com.jetbrains.otp.diagnostic"

fun diagnosticMeter(): Meter = GlobalOpenTelemetry.get().getMeter(DIAGNOSTIC_METER_NAME)
