package ru.kubsu.borshchevyk.core.security

import java.security.KeyPair
import java.security.PublicKey
import javax.crypto.SecretKey

/**
 * Manages cryptographic operations, including RSA key generation, encryption, and signing.
 *
 * This interface abstracts the underlying Android Keystore and standard Java Cryptography
 * Architecture (JCA) APIs. It provides mechanisms to securely generate identities for both
 * offline (Mesh) and online (Server) scenarios, ensuring that private keys are either
 * generated in hardware or securely wrapped for local persistence.
 */
interface KeyManager {
    /**
     * Generates an RSA key pair in memory (not hardware-backed).
     *
     * This is used during online registration where the private key must be encrypted
     * and sent to the server as a backup. Since Android Keystore prevents extracting
     * private keys, an in-memory generation is required for this specific flow.
     *
     * @return the generated [KeyPair]
     */
    fun generateInMemoryRsaKeyPair(): KeyPair

    /**
     * Generates an RSA key pair directly within the hardware-backed Android Keystore.
     *
     * This key never leaves the device and is used for offline mode (Mesh networks)
     * where server backup is not required.
     *
     * @param alias the unique Keystore alias under which the key pair will be stored
     * @return the public part of the generated key pair
     */
    fun generateKeystoreRsaKeyPair(alias: String): PublicKey

    /**
     * Retrieves the public key for a given alias from the Android Keystore.
     *
     * @param alias the Keystore alias
     * @return the [PublicKey] if found, or `null` if the alias does not exist
     */
    fun getPublicKey(alias: String): PublicKey?

    /**
     * Encrypts arbitrary data using an AES-GCM symmetric key derived from a user password.
     *
     * The key derivation utilizes PBKDF2 with HMAC-SHA256 and a random salt. The resulting
     * payload includes the salt, the initialization vector (IV), and the ciphertext.
     *
     * @param data the raw bytes to encrypt (e.g., a PKCS#8 encoded private key)
     * @param password the user's password used for key derivation
     * @return the encrypted byte array containing salt, IV, and ciphertext
     */
    fun encryptWithPassword(data: ByteArray, password: String): ByteArray

    /**
     * Decrypts a payload previously encrypted with a password-derived symmetric key.
     *
     * @param encryptedData the payload containing salt, IV, and ciphertext
     * @param password the user's password used for key derivation
     * @return the decrypted raw bytes
     * @throws javax.crypto.AEADBadTagException if the password is wrong or data is tampered
     */
    fun decryptWithPassword(encryptedData: ByteArray, password: String): ByteArray

    /**
     * Retrieves or creates a hardware-backed AES/GCM secret key in the Android Keystore.
     *
     * This key is used for the "Key Wrapper" pattern to securely store sensitive data
     * (like an RSA private key) locally without requiring the user to enter their password.
     *
     * @param alias the unique Keystore alias for the symmetric key
     * @return the hardware-backed [SecretKey]
     */
    fun getOrCreateLocalSymmetricKey(alias: String): SecretKey

    /**
     * Encrypts raw key bytes using a local, hardware-backed AES key from the Android Keystore.
     *
     * This implements the "Key Wrapper" pattern. The returned payload can be safely
     * persisted in local storage (e.g., DataStore).
     *
     * @param alias the Keystore alias of the AES wrapper key
     * @param keyBytes the raw bytes to wrap (encrypt)
     * @return the wrapped blob containing the IV and ciphertext
     */
    fun wrapKeyWithLocalKeystore(alias: String, keyBytes: ByteArray): ByteArray

    /**
     * Decrypts a wrapped payload using the local, hardware-backed AES key.
     *
     * @param alias the Keystore alias of the AES wrapper key
     * @param wrappedKeyBytes the encrypted blob containing the IV and ciphertext
     * @return the unwrapped raw bytes
     */
    fun unwrapKeyWithLocalKeystore(alias: String, wrappedKeyBytes: ByteArray): ByteArray

    /**
     * Signs the given data using raw RSA private key bytes provided in memory.
     *
     * This is used when the private key was fetched from the server, decrypted, and
     * held in memory temporarily to complete a challenge-response authentication.
     *
     * @param privateKeyBytes the raw PKCS#8 encoded RSA private key
     * @param data the data to sign
     * @return the cryptographic signature (SHA256withRSA)
     */
    fun signDataWithRawKey(privateKeyBytes: ByteArray, data: ByteArray): ByteArray

    /**
     * Signs the given data using the RSA private key securely stored in the Android Keystore.
     *
     * @param alias the Keystore alias of the RSA key pair
     * @param data the data to sign
     * @return the cryptographic signature (SHA256withRSA)
     * @throws IllegalStateException if the key is not found in the Keystore
     */
    fun signData(alias: String, data: ByteArray): ByteArray
    /**
     * Verifies the cryptographic signature of the given data using raw RSA public key bytes.
     *
     * @param publicKeyBytes the raw X.509 encoded RSA public key
     * @param data the original data that was signed
     * @param signature the cryptographic signature to verify
     * @return true if the signature is valid and matches the data, false otherwise
     */
    fun verifyDataWithRawPublicKey(publicKeyBytes: ByteArray, data: ByteArray, signature: ByteArray): Boolean
}
