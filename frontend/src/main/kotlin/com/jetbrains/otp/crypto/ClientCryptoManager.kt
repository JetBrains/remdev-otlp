package com.jetbrains.otp.crypto

import java.security.KeyPair
import javax.crypto.SecretKey

class ClientCryptoManager {
    private val rsaEncryption = RsaEncryption()
    private val encryption = ChaCha20Poly1305Encryption()

    private lateinit var keyPair: KeyPair
    private var sessionKey: SecretKey? = null

    fun initialize(): String {
        keyPair = RsaEncryption.generateKeyPair()
        return RsaEncryption.publicKeyToBase64(keyPair.public)
    }

    fun setKey(encryptedKey: EncryptedKey) {
        val keyBytes = rsaEncryption.decryptKey(encryptedKey, keyPair.private)
        sessionKey = ChaCha20Poly1305Encryption.bytesToKey(keyBytes)
    }

    fun encrypt(data: String): EncryptedData {
        val key = sessionKey ?: throw IllegalStateException("Session key not received yet")
        return encryption.encrypt(data, key)
    }

    fun decrypt(encryptedData: EncryptedData): String {
        val key = sessionKey ?: throw IllegalStateException("Session key not received yet")
        return encryption.decryptToString(encryptedData, key)
    }

    fun isInitialized(): Boolean = sessionKey != null
}
