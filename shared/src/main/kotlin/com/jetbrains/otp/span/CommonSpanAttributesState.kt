package com.jetbrains.otp.span

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes

object CommonSpanAttributesState {
    private val lock = Any()

    @Volatile
    private var attributeMap: Map<String, String> = emptyMap()

    fun put(key: String, value: String) {
        upsert(mapOf(key to value))
    }

    fun upsert(commonSpanAttributes: Map<String, String>) {
        if (commonSpanAttributes.isEmpty()) return

        synchronized(lock) {
            attributeMap = attributeMap + commonSpanAttributes
        }
    }

    fun snapshot(): Attributes = toAttributes(attributeMap)

    fun snapshotMap(): Map<String, String> = attributeMap

    private fun toAttributes(source: Map<String, String>): Attributes {
        if (source.isEmpty()) return Attributes.empty()

        val builder = Attributes.builder()
        source.forEach { (key, value) ->
            builder.put(AttributeKey.stringKey(key), value)
        }
        return builder.build()
    }
}