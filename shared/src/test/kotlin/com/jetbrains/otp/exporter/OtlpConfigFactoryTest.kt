package com.jetbrains.otp.exporter

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OtlpConfigFactoryTest {
    private var originalMetricAttributesProperty: String? = null

    @Before
    fun setUp() {
        originalMetricAttributesProperty = System.getProperty(COMMON_METRIC_ATTRIBUTES_PROPERTY)
        System.clearProperty(COMMON_METRIC_ATTRIBUTES_PROPERTY)
    }

    @After
    fun tearDown() {
        if (originalMetricAttributesProperty == null) {
            System.clearProperty(COMMON_METRIC_ATTRIBUTES_PROPERTY)
        } else {
            System.setProperty(COMMON_METRIC_ATTRIBUTES_PROPERTY, originalMetricAttributesProperty)
        }
    }

    @Test
    fun `reads configured metric attributes from property`() {
        System.setProperty(
            COMMON_METRIC_ATTRIBUTES_PROPERTY,
            "deployment.environment=staging,service.instance.id=rd-backend-1"
        )

        assertEquals(
            mapOf(
                "deployment.environment" to "staging",
                "service.instance.id" to "rd-backend-1",
            ),
            readConfiguredMetricAttributesFromPropertyOrEnv()
        )
    }

    @Test
    fun `metric attributes extend and override common span attributes`() {
        val config = testConfig(
            configuredSpanAttributes = mapOf(
                "span.only" to "span",
                "shared" to "span",
            ),
            configuredMetricAttributes = mapOf(
                "metric.only" to "metric",
                "shared" to "metric",
            )
        )

        assertEquals(
            mapOf(
                "span.only" to "span",
                "shared" to "metric",
                "metric.only" to "metric",
            ),
            config.configuredAttributesForMetrics()
        )
    }

    private fun testConfig(
        configuredSpanAttributes: Map<String, String> = emptyMap(),
        configuredMetricAttributes: Map<String, String> = emptyMap(),
    ): OtlpConfig {
        return OtlpConfig(
            endpoint = "http://localhost",
            headers = emptyMap(),
            traceHeaders = emptyMap(),
            metricHeaders = emptyMap(),
            protocol = OtlpProtocol.HTTP_PROTOBUF,
            timeoutSeconds = 10,
            isPluginSpanFilterEnabled = true,
            isMetricsExportEnabled = true,
            configuredSpanAttributes = configuredSpanAttributes,
            configuredMetricAttributes = configuredMetricAttributes,
        )
    }
}
