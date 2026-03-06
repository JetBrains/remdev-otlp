package com.jetbrains.otp.crypto
import javax.crypto.SecretKey

class ServerCryptoManager {
    private val rsaEncryption = RsaEncryption()
    private val encryption = ChaCha20Poly1305Encryption()

    private val sessionKey: SecretKey = ChaCha20Poly1305Encryption.generateKey()

    fun encryptKeyForClient(clientPublicKeyBase64: String): EncryptedKey {
        val clientPublicKey = RsaEncryption.base64ToPublicKey(clientPublicKeyBase64)
        val keyBytes = ChaCha20Poly1305Encryption.keyToBytes(sessionKey)
        return rsaEncryption.encryptKey(keyBytes, clientPublicKey)
    }

    fun encrypt(data: String): EncryptedData {
        return encryption.encrypt(data, sessionKey)
    }

    fun decrypt(encryptedData: EncryptedData): String {
        return encryption.decryptToString(encryptedData, sessionKey)
    }

    fun getSessionKey(): SecretKey = sessionKey
}
