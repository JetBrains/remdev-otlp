package com.jetbrains.otp.crypto

import kotlinx.serialization.Serializable

@Serializable
data class OtlpRemoteConfig(
    val endpoint: String,
    val encryptedHeaders: EncryptedData,
    val isPluginSpanFilterEnabled: Boolean
)