package com.jetbrains.otp.crypto

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class EncryptedData(
    val data: String,
    val iv: String
) {
    companion object {
        fun fromBytes(data: ByteArray, iv: ByteArray): EncryptedData {
            return EncryptedData(
                data = Base64.getEncoder().encodeToString(data),
                iv = Base64.getEncoder().encodeToString(iv)
            )
        }
    }

    fun getDataBytes(): ByteArray = Base64.getDecoder().decode(data)
    fun getIvBytes(): ByteArray = Base64.getDecoder().decode(iv)
}

@Serializable
data class EncryptedAesKey(
    val encryptedKey: String
) {
    companion object {
        fun fromBytes(encryptedKey: ByteArray): EncryptedAesKey {
            return EncryptedAesKey(
                encryptedKey = Base64.getEncoder().encodeToString(encryptedKey)
            )
        }
    }

    fun getKeyBytes(): ByteArray = Base64.getDecoder().decode(encryptedKey)
}