package io.paladin.sdk.crypto;

import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.digests.KeccakDigest;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.*;
import java.util.Arrays;

/**
 * Local signer using the secp256k1 elliptic curve (the same curve used by Ethereum).
 *
 * <p>This implementation uses BouncyCastle to perform ECDSA signing entirely on the
 * client side, without transmitting private key material to the Paladin node. This
 * enables enterprise-grade key management patterns:
 *
 * <ul>
 *   <li>Air-gapped signing with cold wallets</li>
 *   <li>Integration with Hardware Security Modules (HSMs)</li>
 *   <li>Offline transaction preparation and signing</li>
 *   <li>Multi-signature workflows</li>
 * </ul>
 *
 * <p><strong>Example:</strong></p>
 * <pre>{@code
 * // Generate a new random key pair
 * Secp256k1Signer signer = Secp256k1Signer.generate();
 * System.out.println("Address: " + signer.getAddress());
 *
 * // Or import an existing private key
 * Secp256k1Signer imported = Secp256k1Signer.fromPrivateKey("0xac0974bec...");
 *
 * // Sign a payload
 * byte[] txHash = keccak256(transactionBytes);
 * SignedPayload signed = signer.sign(txHash);
 * }</pre>
 *
 * @see LocalSigner
 * @see SignedPayload
 */
public final class Secp256k1Signer implements LocalSigner {

    private static final Logger log = LoggerFactory.getLogger(Secp256k1Signer.class);
    private static final String ALGORITHM = "secp256k1";

    private static final X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1");
    private static final ECDomainParameters DOMAIN =
            new ECDomainParameters(CURVE_PARAMS.getCurve(), CURVE_PARAMS.getG(),
                    CURVE_PARAMS.getN(), CURVE_PARAMS.getH());

    /** The order (N) divided by 2 — used to enforce low-S signatures (EIP-2). */
    private static final BigInteger HALF_CURVE_ORDER = CURVE_PARAMS.getN().shiftRight(1);

    private final BigInteger privateKey;
    private final byte[] publicKeyBytes;
    private final String address;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Creates a signer from a raw private key.
     *
     * @param privateKey the secp256k1 private key as a {@link BigInteger}
     * @throws CryptoException if the key is invalid
     */
    public Secp256k1Signer(BigInteger privateKey) {
        if (privateKey == null || privateKey.signum() <= 0
                || privateKey.compareTo(CURVE_PARAMS.getN()) >= 0) {
            throw new CryptoException("Invalid secp256k1 private key");
        }
        this.privateKey = privateKey;
        this.publicKeyBytes = derivePublicKey(privateKey);
        this.address = deriveAddress(publicKeyBytes);
        log.debug("Initialized Secp256k1Signer with address: {}", address);
    }

    /**
     * Generates a new random secp256k1 key pair.
     *
     * @return a new signer with a randomly generated private key
     */
    public static Secp256k1Signer generate() {
        SecureRandom random = new SecureRandom();
        byte[] privKeyBytes = new byte[32];
        BigInteger privKey;
        do {
            random.nextBytes(privKeyBytes);
            privKey = new BigInteger(1, privKeyBytes);
        } while (privKey.signum() <= 0 || privKey.compareTo(CURVE_PARAMS.getN()) >= 0);
        return new Secp256k1Signer(privKey);
    }

    /**
     * Creates a signer from a hex-encoded private key string.
     *
     * @param hexPrivateKey the private key in hex format (with or without "0x" prefix)
     * @return a new signer initialized with the given key
     * @throws CryptoException if the key is invalid
     */
    public static Secp256k1Signer fromPrivateKey(String hexPrivateKey) {
        byte[] keyBytes = HexUtils.fromHex(hexPrivateKey);
        return new Secp256k1Signer(new BigInteger(1, keyBytes));
    }

    /**
     * Creates a signer from raw private key bytes.
     *
     * @param privateKeyBytes the 32-byte private key
     * @return a new signer initialized with the given key
     * @throws CryptoException if the key is invalid
     */
    public static Secp256k1Signer fromPrivateKey(byte[] privateKeyBytes) {
        return new Secp256k1Signer(new BigInteger(1, privateKeyBytes));
    }

    @Override
    public SignedPayload sign(byte[] data) {
        try {
            ECDSASigner signer = new ECDSASigner();
            ECPrivateKeyParameters privKeyParams = new ECPrivateKeyParameters(privateKey, DOMAIN);
            signer.init(true, privKeyParams);

            BigInteger[] components = signer.generateSignature(data);
            BigInteger r = components[0];
            BigInteger s = components[1];

            if (s.compareTo(HALF_CURVE_ORDER) > 0) {
                s = CURVE_PARAMS.getN().subtract(s);
            }

            byte[] rBytes = toFixedLengthBytes(r, 32);
            byte[] sBytes = toFixedLengthBytes(s, 32);
            byte[] signature = new byte[64];
            System.arraycopy(rBytes, 0, signature, 0, 32);
            System.arraycopy(sBytes, 0, signature, 32, 32);

            log.debug("Signed {} bytes with address {}", data.length, address);
            return new SignedPayload(data, signature, publicKeyBytes, ALGORITHM);

        } catch (Exception e) {
            throw new CryptoException("Failed to sign data with secp256k1", e);
        }
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public String getAlgorithm() {
        return ALGORITHM;
    }

    @Override
    public String getPublicKeyHex() {
        return "0x" + HexUtils.toHex(publicKeyBytes);
    }

    /**
     * Returns the hex-encoded private key (without "0x" prefix).
     *
     * <p><strong>WARNING:</strong> This method exposes the raw private key.
     * Use with extreme caution and never log or transmit the result.
     *
     * @return the hex-encoded private key
     */
    public String getPrivateKeyHex() {
        return HexUtils.toHex(toFixedLengthBytes(privateKey, 32));
    }

    /**
     * Computes the Keccak-256 hash of the given input.
     *
     * <p>This is the same hash function used by Ethereum for address derivation
     * and transaction hashing.
     *
     * @param input the bytes to hash
     * @return the 32-byte Keccak-256 digest
     */
    public static byte[] keccak256(byte[] input) {
        KeccakDigest digest = new KeccakDigest(256);
        digest.update(input, 0, input.length);
        byte[] result = new byte[32];
        digest.doFinal(result, 0);
        return result;
    }

    /**
     * Verifies an ECDSA signature against the given data and public key.
     *
     * @param data      the original signed data
     * @param signature the 64-byte R+S signature
     * @param publicKey the signer's uncompressed public key (65 bytes with 0x04 prefix)
     * @return {@code true} if the signature is valid
     */
    public static boolean verify(byte[] data, byte[] signature, byte[] publicKey) {
        try {
            BigInteger r = new BigInteger(1, Arrays.copyOfRange(signature, 0, 32));
            BigInteger s = new BigInteger(1, Arrays.copyOfRange(signature, 32, 64));

            ECPoint point = DOMAIN.getCurve().decodePoint(publicKey);
            ECPublicKeyParameters pubKeyParams = new ECPublicKeyParameters(point, DOMAIN);

            ECDSASigner verifier = new ECDSASigner();
            verifier.init(false, pubKeyParams);
            return verifier.verifySignature(data, r, s);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] derivePublicKey(BigInteger privateKey) {
        ECPoint point = new FixedPointCombMultiplier().multiply(DOMAIN.getG(), privateKey);
        return point.getEncoded(false);
    }

    private static String deriveAddress(byte[] publicKey) {
        byte[] pubKeyNoPrefix = Arrays.copyOfRange(publicKey, 1, publicKey.length);
        byte[] hash = keccak256(pubKeyNoPrefix);
        byte[] addressBytes = Arrays.copyOfRange(hash, 12, 32);
        return "0x" + HexUtils.toHex(addressBytes);
    }

    private static byte[] toFixedLengthBytes(BigInteger value, int length) {
        byte[] bytes = value.toByteArray();
        if (bytes.length == length) {
            return bytes;
        }
        byte[] result = new byte[length];
        if (bytes.length > length) {
            System.arraycopy(bytes, bytes.length - length, result, 0, length);
        } else {
            System.arraycopy(bytes, 0, result, length - bytes.length, bytes.length);
        }
        return result;
    }
}
