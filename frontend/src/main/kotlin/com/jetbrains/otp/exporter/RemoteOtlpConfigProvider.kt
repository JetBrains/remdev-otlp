package com.jetbrains.otp.exporter

class RemoteOtlpConfigProvider : OtlpConfigProvider {
    private val configFactory = FromBackendOtlpConfigFactory()

    override suspend fun createConfig(): OtlpConfig {
        return configFactory.createConfig()
    }
}