package io.paladin.sdk.crypto;

/**
 * Interface for local (offline) transaction signing.
 *
 * <p>Implementations sign raw byte payloads using a locally-held private key,
 * enabling enterprise workflows where private keys are never transmitted to
 * the Paladin node. This is critical for environments using HSMs, secure enclaves,
 * or cold wallets.
 *
 * <p><strong>Example:</strong></p>
 * <pre>{@code
 * LocalSigner signer = Secp256k1Signer.generate();
 * SignedPayload signed = signer.sign(transactionBytes);
 * System.out.println("Signature: " + signed.signatureHex());
 * System.out.println("Public Key: " + signed.publicKeyHex());
 * }</pre>
 *
 * @see Secp256k1Signer
 * @see SignedPayload
 */
public interface LocalSigner {

    /**
     * Signs the given raw data and returns the signature along with public key metadata.
     *
     * @param data the raw byte payload to sign (e.g., a transaction hash or RLP-encoded tx)
     * @return a {@link SignedPayload} containing the original data, the signature bytes,
     *         and the signer's public key
     * @throws CryptoException if signing fails due to a key or algorithm error
     */
    SignedPayload sign(byte[] data);

    /**
     * Returns the Ethereum-style address derived from this signer's public key.
     *
     * <p>The address is the last 20 bytes of the Keccak-256 hash of the uncompressed
     * public key (without the 0x04 prefix), prefixed with "0x".
     *
     * @return the hex-encoded Ethereum address (e.g., "0x1a2b3c...")
     */
    String getAddress();

    /**
     * Returns the algorithm name used by this signer (e.g., "secp256k1").
     *
     * @return the algorithm identifier string
     */
    String getAlgorithm();

    /**
     * Returns the hex-encoded public key for this signer.
     *
     * @return the uncompressed public key in hex format
     */
    String getPublicKeyHex();
}
