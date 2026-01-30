package com.jetbrains.otp.crypto

import java.security.*
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher

class RsaEncryption {
    companion object {
        private const val RSA_KEY_SIZE = 2048
        private const val ALGORITHM = "RSA"
        private const val TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"

        fun generateKeyPair(): KeyPair {
            val keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM)
            keyPairGenerator.initialize(RSA_KEY_SIZE, SecureRandom())
            return keyPairGenerator.generateKeyPair()
        }

        fun publicKeyToBase64(publicKey: PublicKey): String {
            return Base64.getEncoder().encodeToString(publicKey.encoded)
        }

        fun base64ToPublicKey(base64: String): PublicKey {
            val keyBytes = Base64.getDecoder().decode(base64)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance(ALGORITHM)
            return keyFactory.generatePublic(keySpec)
        }
    }

    fun encryptAesKey(aesKey: ByteArray, publicKey: PublicKey): EncryptedAesKey {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encryptedKey = cipher.doFinal(aesKey)
        return EncryptedAesKey.fromBytes(encryptedKey)
    }

    fun decryptAesKey(encryptedAesKey: EncryptedAesKey, privateKey: PrivateKey): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher.doFinal(encryptedAesKey.getKeyBytes())
    }
}
