package com.jetbrains.otp.crypto

import com.jetbrains.otp.crypto.rpc.CryptoRpc

internal class CryptoRpcImpl : CryptoRpc {
    override suspend fun requestKeyExchange(clientPublicKey: String): EncryptedAesKey {
        return ServerCryptoManager().encryptAesKeyForClient(clientPublicKey)
    }

    override suspend fun sendEncryptedData(data: EncryptedData): String {
        return ServerCryptoManager().decrypt(data)
    }
}