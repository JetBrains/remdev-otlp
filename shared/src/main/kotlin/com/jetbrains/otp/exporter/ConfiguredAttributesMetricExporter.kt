package com.jetbrains.otp.exporter

import com.jetbrains.otp.span.CommonSpanAttributesState
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.InstrumentType
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.sdk.resources.Resource

class ConfiguredAttributesMetricExporter(
    private val delegate: MetricExporter,
    configuredAttributes: Map<String, String>,
    private val runtimeAttributesProvider: () -> Attributes = CommonSpanAttributesState::snapshot,
) : MetricExporter {
    private val configuredAttributes = toAttributes(configuredAttributes)

    override fun export(metrics: Collection<MetricData>): CompletableResultCode {
        if (metrics.isEmpty()) {
            return delegate.export(metrics)
        }

        val additionalAttributes = mergeConfiguredAndRuntimeAttributes()
        val metricsToExport = if (additionalAttributes.isEmpty) metrics
        else metrics.map { MetricDataWithConfiguredAttributes(it, additionalAttributes) }

        return delegate.export(metricsToExport)
    }

    override fun flush(): CompletableResultCode = delegate.flush()

    override fun shutdown(): CompletableResultCode = delegate.shutdown()
    override fun getAggregationTemporality(instrumentType: InstrumentType) =
        delegate.getAggregationTemporality(instrumentType)

    private fun mergeConfiguredAndRuntimeAttributes(): Attributes {
        val runtimeAttributes = runtimeAttributesProvider()
        if (configuredAttributes.isEmpty && runtimeAttributes.isEmpty) {
            return Attributes.empty()
        }

        return configuredAttributes.toBuilder()
            .putAll(runtimeAttributes)
            .build()
    }
}

private class MetricDataWithConfiguredAttributes(
    private val delegate: MetricData,
    private val additionalAttributes: Attributes,
) : MetricData by delegate {
    override fun getResource(): Resource {
        return delegate.resource.merge(Resource.create(additionalAttributes))
    }
}

private fun toAttributes(source: Map<String, String>): Attributes {
    if (source.isEmpty()) return Attributes.empty()

    val builder = Attributes.builder()
    source.forEach { (key, value) ->
        builder.put(AttributeKey.stringKey(key), value)
    }
    return builder.build()
}
