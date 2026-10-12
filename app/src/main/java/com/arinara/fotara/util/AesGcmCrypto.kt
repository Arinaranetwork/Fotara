// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AesGcmCrypto {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12
    private const val DEFAULT_SEED = "FotaraPrivateSpaceVault2026SecretSeedKey!"

    fun deriveKey(passphrase: String = DEFAULT_SEED): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(passphrase.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(plaintext: ByteArray, key: SecretKeySpec = deriveKey()): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    fun decrypt(encryptedData: ByteArray, key: SecretKeySpec = deriveKey()): ByteArray {
        if (encryptedData.size < IV_LENGTH_BYTES + 16) {
            throw IllegalArgumentException("Encrypted data payload is too short")
        }
        val iv = encryptedData.copyOfRange(0, IV_LENGTH_BYTES)
        val ciphertext = encryptedData.copyOfRange(IV_LENGTH_BYTES, encryptedData.size)
        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(ciphertext)
    }
}
