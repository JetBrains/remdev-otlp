package com.jetbrains.otp.crypto

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.jetbrains.otp.crypto.rpc.CryptoRpc

@Service(Service.Level.APP)
class FrontendCryptoClient {
    private val clientCryptoManager = ClientCryptoManager()

    suspend fun initialize() {
        val cryptoRpc = CryptoRpc.getInstance()
        val clientPublicKey = clientCryptoManager.initialize()
        val encryptedKey = cryptoRpc.requestKeyExchange(clientPublicKey)
        clientCryptoManager.setKey(encryptedKey)
    }

    suspend fun sendEncryptedData(data: String): String {
        if (!clientCryptoManager.isInitialized()) {
            throw IllegalStateException("Crypto client not initialized")
        }
        val encrypted = clientCryptoManager.encrypt(data)
        return CryptoRpc.getInstance().sendEncryptedData(encrypted)
    }

    fun decryptData(encryptedData: EncryptedData): String {
        if (!clientCryptoManager.isInitialized()) {
            throw IllegalStateException("Crypto client not initialized")
        }
        return clientCryptoManager.decrypt(encryptedData)
    }

    fun isInitialized(): Boolean = clientCryptoManager.isInitialized()

    companion object {
        fun getInstance(): FrontendCryptoClient = service()
    }
}
