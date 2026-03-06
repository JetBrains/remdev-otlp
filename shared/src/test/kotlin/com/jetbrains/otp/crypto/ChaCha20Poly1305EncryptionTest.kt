package com.jetbrains.otp.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import javax.crypto.AEADBadTagException

class ChaCha20Poly1305EncryptionTest {
    private val encryption = ChaCha20Poly1305Encryption()

    @Test
    fun `encrypt and decrypt round-trip`() {
        val key = ChaCha20Poly1305Encryption.generateKey()
        val plaintext = "authorization=Bearer secret-token"

        val encrypted = encryption.encrypt(plaintext, key)

        assertEquals(plaintext, encryption.decryptToString(encrypted, key))
    }

    @Test
    fun `encrypt uses a fresh nonce`() {
        val key = ChaCha20Poly1305Encryption.generateKey()
        val plaintext = "authorization=Bearer secret-token"

        val first = encryption.encrypt(plaintext, key)
        val second = encryption.encrypt(plaintext, key)

        assertNotEquals(first.iv, second.iv)
        assertNotEquals(first.data, second.data)
    }

    @Test(expected = AEADBadTagException::class)
    fun `decrypt fails with wrong key`() {
        val plaintext = "authorization=Bearer secret-token"
        val encrypted = encryption.encrypt(plaintext, ChaCha20Poly1305Encryption.generateKey())

        encryption.decryptToString(encrypted, ChaCha20Poly1305Encryption.generateKey())
    }
}
