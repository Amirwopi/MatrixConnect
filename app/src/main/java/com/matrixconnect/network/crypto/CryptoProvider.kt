package com.matrixconnect.network.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

interface CryptoProvider {
    fun encrypt(data: ByteArray): ByteArray
    fun decrypt(data: ByteArray, offset: Int, length: Int): ByteArray

    companion object {
        fun create(type: String, key: String): CryptoProvider {
            return when (type.uppercase()) {
                "AES" -> AESCryptoProvider(key)
                "CHACHA20" -> ChaCha20CryptoProvider(key)
                "SALSA20" -> Salsa20CryptoProvider(key)
                else -> throw IllegalArgumentException("Unsupported encryption type: $type")
            }
        }
    }
}

sealed class CryptoException(message: String) : Exception(message) {
    class EncryptionFailed : CryptoException("Encryption failed")
    class DecryptionFailed : CryptoException("Decryption failed")
}

abstract class BaseCryptoProvider : CryptoProvider {
    protected fun generateIV(size: Int): ByteArray {
        return ByteArray(size).apply {
            SecureRandom().nextBytes(this)
        }
    }

    companion object {
        private const val KEY_SIZE_BYTES = 32  // 256 bits
    }

    protected fun deriveKey(key: String): ByteArray {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashedKey = digest.digest(key.toByteArray())
        return hashedKey.copyOf(KEY_SIZE_BYTES)
    }
}

class AESCryptoProvider(private val key: String) : BaseCryptoProvider() {
    private val keySpec = SecretKeySpec(deriveKey(key), "AES")

    override fun encrypt(data: ByteArray): ByteArray {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = generateIV(12)
            val gcmSpec = GCMParameterSpec(128, iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
            val encrypted = cipher.doFinal(data)
            
            iv + encrypted
        } catch (e: Exception) {
            throw CryptoException.EncryptionFailed()
        }
    }

    override fun decrypt(data: ByteArray, offset: Int, length: Int): ByteArray {
        return try {
            if (length < 12) throw CryptoException.DecryptionFailed()
            
            val iv = data.copyOfRange(offset, offset + 12)
            val encryptedData = data.copyOfRange(offset + 12, offset + length)
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(128, iv)
            
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
            cipher.doFinal(encryptedData)
        } catch (e: Exception) {
            throw CryptoException.DecryptionFailed()
        }
    }
}

class ChaCha20CryptoProvider(private val key: String) : BaseCryptoProvider() {
    private val keyBytes = deriveKey(key)

    override fun encrypt(data: ByteArray): ByteArray {
        return try {
            val nonce = generateIV(12)
            val cipher = Cipher.getInstance("ChaCha20-Poly1305")
            val keySpec = SecretKeySpec(keyBytes, "ChaCha20")
            val paramSpec = javax.crypto.spec.IvParameterSpec(nonce)
            
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, paramSpec)
            val encrypted = cipher.doFinal(data)
            
            nonce + encrypted
        } catch (e: Exception) {
            throw CryptoException.EncryptionFailed()
        }
    }

    override fun decrypt(data: ByteArray, offset: Int, length: Int): ByteArray {
        return try {
            if (length < 12) throw CryptoException.DecryptionFailed()
            
            val nonce = data.copyOfRange(offset, offset + 12)
            val encryptedData = data.copyOfRange(offset + 12, offset + length)
            
            val cipher = Cipher.getInstance("ChaCha20-Poly1305")
            val keySpec = SecretKeySpec(keyBytes, "ChaCha20")
            val paramSpec = javax.crypto.spec.IvParameterSpec(nonce)
            
            cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec)
            cipher.doFinal(encryptedData)
        } catch (e: Exception) {
            throw CryptoException.DecryptionFailed()
        }
    }
}

class Salsa20CryptoProvider(private val key: String) : BaseCryptoProvider() {
    private val keyBytes = deriveKey(key)

    override fun encrypt(data: ByteArray): ByteArray {
        val nonce = generateIV(8)
        val keyStream = generateKeyStream(nonce, data.size)
        val encrypted = ByteArray(data.size) { i -> (data[i].toInt() xor keyStream[i].toInt()).toByte() }
        return nonce + encrypted
    }

    override fun decrypt(data: ByteArray, offset: Int, length: Int): ByteArray {
        if (length < 8) throw CryptoException.DecryptionFailed()
        
        val nonce = data.copyOfRange(offset, offset + 8)
        val encryptedData = data.copyOfRange(offset + 8, offset + length)
        val keyStream = generateKeyStream(nonce, encryptedData.size)
        
        return ByteArray(encryptedData.size) { i -> (encryptedData[i].toInt() xor keyStream[i].toInt()).toByte() }
    }

    private fun generateKeyStream(nonce: ByteArray, length: Int): ByteArray {
        val result = ByteArray(length)
        val hash = java.security.MessageDigest.getInstance("SHA-256")
        var counter = 0
        
        var remaining = length
        while (remaining > 0) {
            hash.reset()
            hash.update(keyBytes)
            hash.update(nonce)
            hash.update(ByteArray(4) { i -> ((counter shr (i * 8)) and 0xFF).toByte() })
            
            val chunk = hash.digest()
            val copyLength = minOf(remaining, chunk.size)
            System.arraycopy(chunk, 0, result, length - remaining, copyLength)
            
            remaining -= copyLength
            counter++
        }
        
        return result
    }
}
