package com.jetbrains.otp.exporter

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.resources.Resource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class TelemetryAttributeRenamerTest {
    @Test
    fun `applies attribute rename rules`() {
        val attributes = Attributes.builder()
            .put(SERVICE_INSTANCE_ID_ATTRIBUTE_KEY, "123456789")
            .put("other.attribute", "value")
            .build()

        val renamedAttributes = TelemetryAttributeRenamer.rename(attributes)

        assertNull(renamedAttributes.get(SERVICE_INSTANCE_ID_ATTRIBUTE_KEY))
        assertEquals("123456789", renamedAttributes.get(TIMESTAMP_ATTRIBUTE_KEY))
        assertEquals("value", renamedAttributes.get(AttributeKey.stringKey("other.attribute")))
    }

    @Test
    fun `applies attribute rename rules to resources`() {
        val resource = Resource.create(
            Attributes.of(SERVICE_INSTANCE_ID_ATTRIBUTE_KEY, "123456789"),
            "https://opentelemetry.io/schemas/1.27.0"
        )

        val renamedResource = TelemetryAttributeRenamer.rename(resource)

        assertNull(renamedResource.attributes.get(SERVICE_INSTANCE_ID_ATTRIBUTE_KEY))
        assertEquals("123456789", renamedResource.attributes.get(TIMESTAMP_ATTRIBUTE_KEY))
        assertEquals("https://opentelemetry.io/schemas/1.27.0", renamedResource.schemaUrl)
    }

    @Test
    fun `returns same attributes when no rules match`() {
        val attributes = Attributes.of(AttributeKey.stringKey("other.attribute"), "value")

        assertSame(attributes, TelemetryAttributeRenamer.rename(attributes))
    }

    private companion object {
        val SERVICE_INSTANCE_ID_ATTRIBUTE_KEY: AttributeKey<String> = AttributeKey.stringKey("service.instance.id")
        val TIMESTAMP_ATTRIBUTE_KEY: AttributeKey<String> = AttributeKey.stringKey("timestamp")
    }
}