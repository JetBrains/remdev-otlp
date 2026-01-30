package com.jetbrains.otp.crypto

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
class BackendCryptoService {
    private val serverCryptoManager = ServerCryptoManager()

    fun encryptAesKeyForClient(clientPublicKey: String): EncryptedAesKey {
        return serverCryptoManager.encryptAesKeyForClient(clientPublicKey)
    }

    fun decryptData(data: EncryptedData): String {
        return serverCryptoManager.decrypt(data)
    }

    fun encryptData(data: String): EncryptedData {
        return serverCryptoManager.encrypt(data)
    }

    companion object {
        fun getInstance(): BackendCryptoService = service()
    }
}