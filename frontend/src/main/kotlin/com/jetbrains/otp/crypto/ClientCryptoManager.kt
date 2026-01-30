package com.jetbrains.otp.crypto

import java.security.KeyPair
import javax.crypto.SecretKey

class ClientCryptoManager {
    private val rsaEncryption = RsaEncryption()
    private val aesEncryption = AesEncryption()

    private lateinit var keyPair: KeyPair
    private var aesKey: SecretKey? = null

    fun initialize(): String {
        keyPair = RsaEncryption.generateKeyPair()
        return RsaEncryption.publicKeyToBase64(keyPair.public)
    }

    fun setAesKey(encryptedAesKey: EncryptedAesKey) {
        val aesKeyBytes = rsaEncryption.decryptAesKey(encryptedAesKey, keyPair.private)
        aesKey = AesEncryption.bytesToKey(aesKeyBytes)
    }

    fun encrypt(data: String): EncryptedData {
        val key = aesKey ?: throw IllegalStateException("AES key not received yet")
        return aesEncryption.encrypt(data, key)
    }

    fun decrypt(encryptedData: EncryptedData): String {
        val key = aesKey ?: throw IllegalStateException("AES key not received yet")
        return aesEncryption.decryptToString(encryptedData, key)
    }

    fun isInitialized(): Boolean = aesKey != null
}
