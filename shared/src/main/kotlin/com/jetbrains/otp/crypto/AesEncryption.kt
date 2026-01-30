package com.jetbrains.otp.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class AesEncryption {
    companion object {
        private const val AES_KEY_SIZE = 256
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        fun generateKey(): SecretKey {
            val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
            keyGenerator.init(AES_KEY_SIZE, SecureRandom())
            return keyGenerator.generateKey()
        }

        fun keyToBytes(key: SecretKey): ByteArray = key.encoded

        fun bytesToKey(keyBytes: ByteArray): SecretKey {
            return SecretKeySpec(keyBytes, ALGORITHM)
        }
    }

    private val secureRandom = SecureRandom()

    fun encrypt(data: ByteArray, key: SecretKey): EncryptedData {
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

        val encryptedData = cipher.doFinal(data)
        return EncryptedData.fromBytes(encryptedData, iv)
    }

    fun encrypt(text: String, key: SecretKey): EncryptedData {
        return encrypt(text.toByteArray(Charsets.UTF_8), key)
    }

    fun decrypt(encryptedData: EncryptedData, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, encryptedData.getIvBytes())
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

        return cipher.doFinal(encryptedData.getDataBytes())
    }

    fun decryptToString(encryptedData: EncryptedData, key: SecretKey): String {
        return decrypt(encryptedData, key).toString(Charsets.UTF_8)
    }
}