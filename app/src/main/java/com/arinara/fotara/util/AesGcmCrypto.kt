// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object AesGcmCrypto {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12
    private const val SALT_LENGTH_BYTES = 16
    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val DEFAULT_SEED = "FotaraPrivateSpaceVault2026SecretSeedKey!"
    private val V2_MAGIC = byteArrayOf(0x46, 0x54, 0x52, 0x32) // "FTR2"

    fun deriveKey(passphrase: String = DEFAULT_SEED): SecretKeySpec {
        return deriveKeyLegacy(passphrase)
    }

    fun deriveKeyLegacy(passphrase: String = DEFAULT_SEED): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(passphrase.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    fun deriveKeyPbkdf2(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        val secretKeySpec = SecretKeySpec(keyBytes, "AES")
        java.util.Arrays.fill(keyBytes, 0.toByte())
        spec.clearPassword()
        return secretKeySpec
    }

    fun encrypt(plaintext: ByteArray, passphrase: String = DEFAULT_SEED): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES)
        random.nextBytes(salt)

        val iv = ByteArray(IV_LENGTH_BYTES)
        random.nextBytes(iv)

        val key = deriveKeyPbkdf2(passphrase.toCharArray(), salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)

        return V2_MAGIC + salt + iv + ciphertext
    }

    fun decrypt(encryptedData: ByteArray, passphrase: String = DEFAULT_SEED): ByteArray {
        // Check for V2 format with PBKDF2
        if (encryptedData.size >= V2_MAGIC.size + SALT_LENGTH_BYTES + IV_LENGTH_BYTES + 16 &&
            encryptedData.take(V2_MAGIC.size).toByteArray().contentEquals(V2_MAGIC)
        ) {
            val offset = V2_MAGIC.size
            val salt = encryptedData.copyOfRange(offset, offset + SALT_LENGTH_BYTES)
            val iv = encryptedData.copyOfRange(offset + SALT_LENGTH_BYTES, offset + SALT_LENGTH_BYTES + IV_LENGTH_BYTES)
            val ciphertext = encryptedData.copyOfRange(offset + SALT_LENGTH_BYTES + IV_LENGTH_BYTES, encryptedData.size)

            val key = deriveKeyPbkdf2(passphrase.toCharArray(), salt)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            return cipher.doFinal(ciphertext)
        }

        // Legacy V1 fallback (SHA-256 key, [12-byte IV] + [Ciphertext])
        if (encryptedData.size < IV_LENGTH_BYTES + 16) {
            throw IllegalArgumentException("Encrypted data payload is too short")
        }
        val iv = encryptedData.copyOfRange(0, IV_LENGTH_BYTES)
        val ciphertext = encryptedData.copyOfRange(IV_LENGTH_BYTES, encryptedData.size)
        val key = deriveKeyLegacy(passphrase)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return cipher.doFinal(ciphertext)
    }
}
