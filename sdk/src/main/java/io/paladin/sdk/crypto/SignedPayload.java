package io.paladin.sdk.crypto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a signed payload containing the original data, the cryptographic signature,
 * and the public key of the signer.
 *
 * <p>This record is produced by {@link LocalSigner#sign(byte[])} and can be used
 * to submit pre-signed transactions to a Paladin node or to verify signatures locally.
 *
 * @param data         the original raw byte payload that was signed
 * @param signature    the cryptographic signature bytes (DER-encoded for ECDSA)
 * @param publicKey    the signer's uncompressed public key bytes
 * @param algorithm    the signing algorithm used (e.g., "secp256k1")
 */
public record SignedPayload(
        byte[] data,
        byte[] signature,
        byte[] publicKey,
        String algorithm
) {
    /**
     * Returns the signature as a hex-encoded string (prefixed with "0x").
     *
     * @return hex signature string
     */
    public String signatureHex() {
        return "0x" + HexUtils.toHex(signature);
    }

    /**
     * Returns the public key as a hex-encoded string (prefixed with "0x").
     *
     * @return hex public key string
     */
    public String publicKeyHex() {
        return "0x" + HexUtils.toHex(publicKey);
    }

    /**
     * Returns the original data as a hex-encoded string (prefixed with "0x").
     *
     * @return hex data string
     */
    public String dataHex() {
        return "0x" + HexUtils.toHex(data);
    }
}
