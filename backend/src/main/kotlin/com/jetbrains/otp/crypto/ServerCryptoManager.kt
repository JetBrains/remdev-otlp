package com.jetbrains.otp.crypto

import java.security.PublicKey
import javax.crypto.SecretKey

class ServerCryptoManager {
    private val rsaEncryption = RsaEncryption()
    private val aesEncryption = AesEncryption()

    private val aesKey: SecretKey = AesEncryption.generateKey()

    fun encryptAesKeyForClient(clientPublicKeyBase64: String): EncryptedAesKey {
        val clientPublicKey = RsaEncryption.base64ToPublicKey(clientPublicKeyBase64)
        val aesKeyBytes = AesEncryption.keyToBytes(aesKey)
        return rsaEncryption.encryptAesKey(aesKeyBytes, clientPublicKey)
    }

    fun encrypt(data: String): EncryptedData {
        return aesEncryption.encrypt(data, aesKey)
    }

    fun decrypt(encryptedData: EncryptedData): String {
        return aesEncryption.decryptToString(encryptedData, aesKey)
    }

    fun getAesKey(): SecretKey = aesKey
}