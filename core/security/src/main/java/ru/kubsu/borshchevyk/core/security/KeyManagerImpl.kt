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
    private val secureRandom = SecureRandom()

    companion object {
        private const val PBKDF2_ITERATIONS = 100_000
        private const val AES_KEY_SIZE = 256
        private const val GCM_IV_SIZE = 12
        private const val GCM_TAG_SIZE = 128
        private const val SALT_SIZE = 16
        private const val RSA_KEY_SIZE = 2048
    }

    /**
     * Generates an RSA key pair in memory using the standard JCA provider.
     *
     * @return the generated [KeyPair].
     */
    override fun generateInMemoryRsaKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA)
        kpg.initialize(RSA_KEY_SIZE)
        return kpg.generateKeyPair()
    }

    /**
     * Generates an RSA key pair directly within the hardware-backed "AndroidKeyStore".
     *
     * @param alias the unique Keystore alias under which the key pair will be stored.
     * @return the public part of the generated key pair.
     */
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

    /**
     * Retrieves the public key for a given alias from the Android Keystore.
     *
     * @param alias the Keystore alias.
     * @return the [PublicKey] if found, or `null` if the alias does not exist.
     */
    override fun getPublicKey(alias: String): PublicKey? {
        if (!keyStore.containsAlias(alias)) return null
        val entry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
        return entry?.certificate?.publicKey
    }

    /**
     * Encrypts arbitrary data using an AES-GCM symmetric key derived from a user password.
     *
     * @param data the raw bytes to encrypt.
     * @param password the user's password used for key derivation.
     * @return the encrypted byte array containing salt, IV, and ciphertext.
     */
    override fun encryptWithPassword(data: ByteArray, password: String): ByteArray {
        val salt = ByteArray(SALT_SIZE)
        secureRandom.nextBytes(salt)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_SIZE)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv ?: throw IllegalStateException("Cipher failed to generate IV")
        val encrypted = cipher.doFinal(data)

        val buffer = ByteBuffer.allocate(salt.size + iv.size + encrypted.size)
        buffer.put(salt)
        buffer.put(iv)
        buffer.put(encrypted)
        return buffer.array()
    }

    /**
     * Decrypts a payload previously encrypted with a password-derived symmetric key.
     *
     * @param encryptedData the payload containing salt, IV, and ciphertext.
     * @param password the user's password used for key derivation.
     * @return the decrypted raw bytes.
     * @throws IllegalArgumentException if the encrypted data is too short.
     */
    override fun decryptWithPassword(encryptedData: ByteArray, password: String): ByteArray {
        if (encryptedData.size < SALT_SIZE + GCM_IV_SIZE) {
            throw IllegalArgumentException("Encrypted data is too short")
        }
        
        val buffer = ByteBuffer.wrap(encryptedData)
        val salt = ByteArray(SALT_SIZE)
        buffer.get(salt)
        val iv = ByteArray(GCM_IV_SIZE)
        buffer.get(iv)
        val encrypted = ByteArray(buffer.remaining())
        buffer.get(encrypted)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_SIZE)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        return cipher.doFinal(encrypted)
    }

    /**
     * Retrieves or creates a hardware-backed AES/GCM secret key in the Android Keystore.
     *
     * @param alias the unique Keystore alias for the symmetric key.
     * @return the hardware-backed [SecretKey].
     */
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
            .setKeySize(AES_KEY_SIZE)
            .build()
        
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts raw key bytes using a local, hardware-backed AES key from the Android Keystore.
     *
     * @param alias the Keystore alias of the AES wrapper key.
     * @param keyBytes the raw bytes to wrap (encrypt).
     * @return the wrapped blob containing the IV and ciphertext.
     * @throws IllegalStateException if the cipher failed to generate an IV.
     */
    override fun wrapKeyWithLocalKeystore(alias: String, keyBytes: ByteArray): ByteArray {
        val secretKey = getOrCreateLocalSymmetricKey(alias)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv ?: throw IllegalStateException("Cipher failed to generate IV")
        val encrypted = cipher.doFinal(keyBytes)

        val buffer = ByteBuffer.allocate(iv.size + encrypted.size)
        buffer.put(iv)
        buffer.put(encrypted)
        return buffer.array()
    }

    /**
     * Decrypts a wrapped payload using the local, hardware-backed AES key.
     *
     * @param alias the Keystore alias of the AES wrapper key.
     * @param wrappedKeyBytes the encrypted blob containing the IV and ciphertext.
     * @return the unwrapped raw bytes.
     * @throws IllegalArgumentException if the wrapped key data is too short.
     */
    override fun unwrapKeyWithLocalKeystore(alias: String, wrappedKeyBytes: ByteArray): ByteArray {
        if (wrappedKeyBytes.size < GCM_IV_SIZE) {
            throw IllegalArgumentException("Wrapped key data is too short")
        }

        val secretKey = getOrCreateLocalSymmetricKey(alias)
        val buffer = ByteBuffer.wrap(wrappedKeyBytes)
        val iv = ByteArray(GCM_IV_SIZE)
        buffer.get(iv)
        val encrypted = ByteArray(buffer.remaining())
        buffer.get(encrypted)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        return cipher.doFinal(encrypted)
    }

    /**
     * Signs the given data using raw RSA private key bytes provided in memory.
     *
     * @param privateKeyBytes the raw PKCS#8 encoded RSA private key.
     * @param data the data to sign.
     * @return the cryptographic signature (SHA256withRSA).
     */
    override fun signDataWithRawKey(privateKeyBytes: ByteArray, data: ByteArray): ByteArray {
        val kf = KeyFactory.getInstance("RSA")
        val privateKey = kf.generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes))
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()
    }

    /**
     * Signs the given data using the RSA private key securely stored in the Android Keystore.
     *
     * @param alias the Keystore alias of the RSA key pair.
     * @param data the data to sign.
     * @return the cryptographic signature (SHA256withRSA).
     * @throws IllegalStateException if the key with the given alias is not found.
     */
    override fun signData(alias: String, data: ByteArray): ByteArray {
        val entry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
            ?: throw IllegalStateException("Key pair for alias $alias not found in AndroidKeyStore")

        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(entry.privateKey)
        signer.update(data)
        return signer.sign()
    }

    /**
     * Verifies the cryptographic signature of the given data using raw RSA public key bytes.
     *
     * @param publicKeyBytes the raw X.509 encoded RSA public key
     * @param data the original data that was signed
     * @param signature the cryptographic signature to verify
     * @return true if the signature is valid and matches the data, false otherwise
     */
    override fun verifyDataWithRawPublicKey(publicKeyBytes: ByteArray, data: ByteArray, signature: ByteArray): Boolean {
        val keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_RSA)
        val publicKeySpec = java.security.spec.X509EncodedKeySpec(publicKeyBytes)
        val publicKey = keyFactory.generatePublic(publicKeySpec)

        val signer = Signature.getInstance("SHA256withRSA")
        signer.initVerify(publicKey)
        signer.update(data)
        return signer.verify(signature)
    }
}