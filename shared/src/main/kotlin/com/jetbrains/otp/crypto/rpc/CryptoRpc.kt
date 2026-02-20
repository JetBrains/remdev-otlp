package com.jetbrains.otp.crypto.rpc

import com.intellij.platform.rpc.RemoteApiProviderService
import com.jetbrains.otp.crypto.EncryptedAesKey
import com.jetbrains.otp.crypto.EncryptedData
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import fleet.rpc.remoteApiDescriptor
import org.jetbrains.annotations.ApiStatus

@Rpc
@Suppress("UnstableApiUsage")
@ApiStatus.Internal
interface CryptoRpc : RemoteApi<Unit> {
    suspend fun requestKeyExchange(clientPublicKey: String): EncryptedAesKey

    suspend fun sendEncryptedData(data: EncryptedData): String

    suspend fun getEncryptedOtlpHeaders(): EncryptedData

    companion object {
        @JvmStatic
        suspend fun getInstance(): CryptoRpc {
            return RemoteApiProviderService.resolve(remoteApiDescriptor<CryptoRpc>())
        }
    }
}