package com.jetbrains.otp.exporter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OtlpConfigFactoryTest {
    @Test
    fun `protocol defaults to http protobuf`() {
        withSystemProperty(OTLP_PROTOCOL_PROPERTY, null) {
            assertEquals(OtlpProtocol.HTTP_PROTOBUF, readOtlpProtocolFromPropertyOrEnv())
        }
    }

    @Test
    fun `protocol reads grpc property override`() {
        withSystemProperty(OTLP_PROTOCOL_PROPERTY, "grpc") {
            assertEquals(OtlpProtocol.GRPC, readOtlpProtocolFromPropertyOrEnv())
        }
    }

    @Test
    fun `invalid protocol falls back to default`() {
        withSystemProperty(OTLP_PROTOCOL_PROPERTY, "http/json") {
            assertEquals(OtlpProtocol.HTTP_PROTOBUF, readOtlpProtocolFromPropertyOrEnv())
        }
    }

    @Test
    fun `http exporters append signal paths`() {
        val config = testConfig(protocol = OtlpProtocol.HTTP_PROTOBUF)

        assertEquals("http://localhost:4318/v1/traces", resolveTraceExporterEndpoint(config))
        assertEquals("http://localhost:4318/v1/metrics", resolveMetricExporterEndpoint(config))
        assertNotNull(OtlpSpanExporterFactory.create(config))
        assertNotNull(OtlpMetricExporterFactory.create(config))
    }

    @Test
    fun `grpc exporters use raw endpoint`() {
        val config = testConfig(protocol = OtlpProtocol.GRPC, endpoint = "http://localhost:4317")

        assertEquals("http://localhost:4317", resolveTraceExporterEndpoint(config))
        assertEquals("http://localhost:4317", resolveMetricExporterEndpoint(config))
        assertNotNull(OtlpSpanExporterFactory.create(config))
        assertNotNull(OtlpMetricExporterFactory.create(config))
    }

    private fun testConfig(
        protocol: OtlpProtocol,
        endpoint: String = "http://localhost:4318"
    ): OtlpConfig {
        return OtlpConfig(
            endpoint = endpoint,
            protocol = protocol,
            headers = emptyMap(),
            timeoutSeconds = 10,
            isPluginSpanFilterEnabled = true,
            isMetricsExportEnabled = true
        )
    }

    private fun withSystemProperty(name: String, value: String?, block: () -> Unit) {
        val previousValue = System.getProperty(name)
        try {
            if (value == null) {
                System.clearProperty(name)
            } else {
                System.setProperty(name, value)
            }
            block()
        } finally {
            if (previousValue == null) {
                System.clearProperty(name)
            } else {
                System.setProperty(name, previousValue)
            }
        }
    }
}
