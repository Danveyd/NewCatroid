package org.catrobat.catroid.utils.lunoscript.security

import java.io.File
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object LunoSecurity {

    private const val KEY_RAW = "ChEngineSecure"
    private const val ALGORITHM = "AES"

    private val secretKey: SecretKeySpec by lazy {
        var keyStr = KEY_RAW
        if (keyStr.length < 16) {
            keyStr = keyStr.padEnd(16, '0')
        } else if (keyStr.length > 16) {
            keyStr = keyStr.substring(0, 16)
        }
        SecretKeySpec(keyStr.toByteArray(StandardCharsets.UTF_8), ALGORITHM)
    }

    fun saveEncrypted(file: File, code: String) {
        val minifiedCode = minify(code)
        val encryptedBytes = encrypt(minifiedCode)
        file.writeBytes(encryptedBytes)
    }

    fun loadDecrypted(file: File): String {
        val bytes = file.readBytes()
        return decrypt(bytes)
    }

    private fun minify(code: String): String {
        return code
    }

    private fun encrypt(input: String): ByteArray {
        try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            return cipher.doFinal(input.toByteArray(StandardCharsets.UTF_8))
        } catch (e: Exception) {
            throw RuntimeException("Encryption failed", e)
        }
    }

    private fun decrypt(input: ByteArray): String {
        try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decodedBytes = cipher.doFinal(input)
            return String(decodedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("Decryption failed (Wrong key or corrupted file)", e)
        }
    }
}