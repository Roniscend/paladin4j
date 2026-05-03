package io.paladin.sdk.crypto;

/**
 * Exception thrown when a cryptographic operation fails.
 *
 * <p>Wraps lower-level exceptions from BouncyCastle or JCA to provide
 * a clean SDK-level error type for callers.
 */
public class CryptoException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Creates a new {@code CryptoException} with the given message. */
    public CryptoException(String message) {
        super(message);
    }

    /** Creates a new {@code CryptoException} with the given message and cause. */
    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
