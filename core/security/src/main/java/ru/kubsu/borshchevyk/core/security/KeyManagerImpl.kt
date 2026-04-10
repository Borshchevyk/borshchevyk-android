package ru.kubsu.borshchevyk.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard implementation of [KeyManager] leveraging Android Keystore and JCA.
 *
 * This implementation enforces security best practices, such as utilizing AES/GCM
 * with random initialization vectors and PBKDF2 for password-based key derivation.
 * Hardware backing (StrongBox/TEE) is inherently used when generating keys via the
 * "AndroidKeyStore" provider.
 */
@Singleton
class KeyManagerImpl @Inject constructor() : KeyManager {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    override fun generateInMemoryRsaKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA)
        kpg.initialize(2048)
        return kpg.generateKeyPair()
    }

    override fun generateKeystoreRsaKeyPair(alias: String): PublicKey {
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .build()

        kpg.initialize(spec)
        return kpg.generateKeyPair().public
    }

    override fun getPublicKey(alias: String): PublicKey? {
        if (!keyStore.containsAlias(alias)) return null
        val entry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
        return entry?.certificate?.publicKey
    }

    override fun encryptWithPassword(data: ByteArray, password: String): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)

        val buffer = ByteBuffer.allocate(salt.size + iv.size + encrypted.size)
        buffer.put(salt)
        buffer.put(iv)
        buffer.put(encrypted)
        return buffer.array()
    }

    override fun decryptWithPassword(encryptedData: ByteArray, password: String): ByteArray {
        val buffer = ByteBuffer.wrap(encryptedData)
        val salt = ByteArray(16)
        buffer.get(salt)
        val iv = ByteArray(12)
        buffer.get(iv)
        val encrypted = ByteArray(buffer.remaining())
        buffer.get(encrypted)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        return cipher.doFinal(encrypted)
    }

    override fun getOrCreateLocalSymmetricKey(alias: String): SecretKey {
        if (keyStore.containsAlias(alias)) {
            val entry = keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    override fun wrapKeyWithLocalKeystore(alias: String, keyBytes: ByteArray): ByteArray {
        val secretKey = getOrCreateLocalSymmetricKey(alias)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(keyBytes)

        val buffer = ByteBuffer.allocate(iv.size + encrypted.size)
        buffer.put(iv)
        buffer.put(encrypted)
        return buffer.array()
    }

    override fun unwrapKeyWithLocalKeystore(alias: String, wrappedKeyBytes: ByteArray): ByteArray {
        val secretKey = getOrCreateLocalSymmetricKey(alias)
        val buffer = ByteBuffer.wrap(wrappedKeyBytes)
        val iv = ByteArray(12)
        buffer.get(iv)
        val encrypted = ByteArray(buffer.remaining())
        buffer.get(encrypted)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        return cipher.doFinal(encrypted)
    }

    override fun signDataWithRawKey(privateKeyBytes: ByteArray, data: ByteArray): ByteArray {
        val kf = KeyFactory.getInstance("RSA")
        val privateKey = kf.generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes))
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()
    }

    override fun signData(alias: String, data: ByteArray): ByteArray {
        val entry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
            ?: throw IllegalStateException("Key with alias $alias not found")

        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(entry.privateKey)
        signature.update(data)
        return signature.sign()
    }
}
