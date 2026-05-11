package com.jetbrains.otp.exporter

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.sdk.resources.Resource

internal object TelemetryAttributeRenamer {
    private val renameRules = listOf<AttributeRenameRule<*>>(
        AttributeRenameRule(
            sourceKey = AttributeKey.stringKey("service.instance.id"),
            targetKey = AttributeKey.stringKey("timestamp")
        )
    )

    fun rename(attributes: Attributes): Attributes {
        var builder: AttributesBuilder? = null

        renameRules.forEach { rule ->
            rule.applyIfPresent(attributes) { builder ?: attributes.toBuilder() }
                ?.let { builder = it }
        }

        return builder?.build() ?: attributes
    }

    fun rename(resource: Resource): Resource {
        val renamedAttributes = rename(resource.attributes)
        if (renamedAttributes === resource.attributes) {
            return resource
        }

        return Resource.create(renamedAttributes, resource.schemaUrl)
    }
}

private data class AttributeRenameRule<T : Any>(
    private val sourceKey: AttributeKey<T>,
    private val targetKey: AttributeKey<T>,
) {
    fun applyIfPresent(attributes: Attributes, builderProvider: () -> AttributesBuilder): AttributesBuilder? {
        val sourceValue = attributes.get(sourceKey) ?: return null
        val builder = builderProvider()

        builder.removeIf { key -> key.key == sourceKey.key }
        builder.put(targetKey, sourceValue)
        return builder
    }
}