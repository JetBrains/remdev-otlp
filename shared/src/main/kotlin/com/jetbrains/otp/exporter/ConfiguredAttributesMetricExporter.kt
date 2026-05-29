package com.jetbrains.otp.exporter

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.InstrumentType
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.sdk.resources.Resource

class ConfiguredAttributesMetricExporter(
    private val delegate: MetricExporter,
    configuredAttributes: Map<String, String>,
) : MetricExporter {
    private val attributesResolver = MetricAttributesResolver(configuredAttributes)

    override fun export(metrics: Collection<MetricData>): CompletableResultCode {
        if (metrics.isEmpty()) {
            return delegate.export(metrics)
        }

        val additionalAttributes = attributesResolver.getAttributes()
        val metricsToExport = metrics.map { MetricDataWithConfiguredAttributes(it, additionalAttributes) }

        return delegate.export(metricsToExport)
    }

    override fun flush(): CompletableResultCode = delegate.flush()

    override fun shutdown(): CompletableResultCode = delegate.shutdown()
    override fun getAggregationTemporality(instrumentType: InstrumentType) =
        delegate.getAggregationTemporality(instrumentType)

}

private class MetricDataWithConfiguredAttributes(
    private val delegate: MetricData,
    private val additionalAttributes: Attributes,
) : MetricData by delegate {
    override fun getResource(): Resource {
        val resource = if (additionalAttributes.isEmpty) {
            delegate.resource
        } else {
            delegate.resource.merge(Resource.create(additionalAttributes))
        }

        return TelemetryAttributeRenamer.rename(resource)
    }
}
