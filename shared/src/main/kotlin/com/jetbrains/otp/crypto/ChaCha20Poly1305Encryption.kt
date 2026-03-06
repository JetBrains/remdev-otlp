package com.jetbrains.otp.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class ChaCha20Poly1305Encryption {
    companion object {
        private const val KEY_ALGORITHM = "ChaCha20"
        private const val TRANSFORMATION = "ChaCha20-Poly1305"
        private const val NONCE_LENGTH = 12

        fun generateKey(): SecretKey {
            val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM)
            keyGenerator.init(256, SecureRandom())
            return keyGenerator.generateKey()
        }

        fun keyToBytes(key: SecretKey): ByteArray = key.encoded

        fun bytesToKey(keyBytes: ByteArray): SecretKey {
            return SecretKeySpec(keyBytes, KEY_ALGORITHM)
        }
    }

    private val secureRandom = SecureRandom()

    fun encrypt(data: ByteArray, key: SecretKey): EncryptedData {
        val nonce = ByteArray(NONCE_LENGTH)
        secureRandom.nextBytes(nonce)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(nonce))

        return EncryptedData.fromBytes(cipher.doFinal(data), nonce)
    }

    fun encrypt(text: String, key: SecretKey): EncryptedData {
        return encrypt(text.toByteArray(Charsets.UTF_8), key)
    }

    fun decrypt(encryptedData: EncryptedData, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(encryptedData.getIvBytes()))
        return cipher.doFinal(encryptedData.getDataBytes())
    }

    fun decryptToString(encryptedData: EncryptedData, key: SecretKey): String {
        return decrypt(encryptedData, key).toString(Charsets.UTF_8)
    }
}
