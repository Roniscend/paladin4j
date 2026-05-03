package io.paladin.sdk.crypto;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for the {@link Secp256k1Signer} local signing implementation.
 */
class Secp256k1SignerTest {

    // Well-known Hardhat/Anvil test private key #0
    private static final String TEST_PRIVATE_KEY =
            "ac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80";
    // The expected Ethereum address for the above key
    private static final String TEST_ADDRESS =
            "0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266";

    @Test
    @DisplayName("generate() creates a valid signer with a unique address")
    void testGenerate() {
        Secp256k1Signer signer1 = Secp256k1Signer.generate();
        Secp256k1Signer signer2 = Secp256k1Signer.generate();

        assertThat(signer1.getAddress()).startsWith("0x").hasSize(42);
        assertThat(signer2.getAddress()).startsWith("0x").hasSize(42);
        assertThat(signer1.getAddress()).isNotEqualTo(signer2.getAddress());
        assertThat(signer1.getAlgorithm()).isEqualTo("secp256k1");
    }

    @Test
    @DisplayName("fromPrivateKey(hex) derives the correct Ethereum address")
    void testFromPrivateKeyHex() {
        Secp256k1Signer signer = Secp256k1Signer.fromPrivateKey(TEST_PRIVATE_KEY);
        assertThat(signer.getAddress()).isEqualToIgnoringCase(TEST_ADDRESS);
    }

    @Test
    @DisplayName("fromPrivateKey with 0x prefix works correctly")
    void testFromPrivateKeyWithPrefix() {
        Secp256k1Signer signer = Secp256k1Signer.fromPrivateKey("0x" + TEST_PRIVATE_KEY);
        assertThat(signer.getAddress()).isEqualToIgnoringCase(TEST_ADDRESS);
    }

    @Test
    @DisplayName("sign() produces a valid 64-byte R+S signature")
    void testSign() {
        Secp256k1Signer signer = Secp256k1Signer.fromPrivateKey(TEST_PRIVATE_KEY);
        byte[] data = Secp256k1Signer.keccak256("Hello Paladin".getBytes());

        SignedPayload signed = signer.sign(data);

        assertThat(signed.signature()).hasSize(64);
        assertThat(signed.publicKey()).hasSize(65); // Uncompressed: 0x04 + 32 + 32
        assertThat(signed.publicKey()[0]).isEqualTo((byte) 0x04);
        assertThat(signed.algorithm()).isEqualTo("secp256k1");
        assertThat(signed.signatureHex()).startsWith("0x");
        assertThat(signed.publicKeyHex()).startsWith("0x");
    }

    @Test
    @DisplayName("sign() produces a deterministic-style signature that verifies correctly")
    void testSignAndVerify() {
        Secp256k1Signer signer = Secp256k1Signer.fromPrivateKey(TEST_PRIVATE_KEY);
        byte[] data = Secp256k1Signer.keccak256("transaction-data-payload".getBytes());

        SignedPayload signed = signer.sign(data);

        boolean valid = Secp256k1Signer.verify(data, signed.signature(), signed.publicKey());
        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("verify() rejects tampered data")
    void testVerifyRejectsTamperedData() {
        Secp256k1Signer signer = Secp256k1Signer.generate();
        byte[] data = Secp256k1Signer.keccak256("original".getBytes());
        byte[] tampered = Secp256k1Signer.keccak256("tampered".getBytes());

        SignedPayload signed = signer.sign(data);

        boolean valid = Secp256k1Signer.verify(tampered, signed.signature(), signed.publicKey());
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("verify() rejects wrong public key")
    void testVerifyRejectsWrongKey() {
        Secp256k1Signer signer1 = Secp256k1Signer.generate();
        Secp256k1Signer signer2 = Secp256k1Signer.generate();
        byte[] data = Secp256k1Signer.keccak256("test".getBytes());

        SignedPayload signed = signer1.sign(data);

        // Verify with wrong public key
        boolean valid = Secp256k1Signer.verify(data, signed.signature(),
                HexUtils.fromHex(signer2.getPublicKeyHex().substring(2)));
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("keccak256 produces correct 32-byte hash")
    void testKeccak256() {
        byte[] hash = Secp256k1Signer.keccak256("".getBytes());
        assertThat(hash).hasSize(32);
        // Well-known Keccak-256 of empty string
        String expected = "c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470";
        assertThat(HexUtils.toHex(hash)).isEqualTo(expected);
    }

    @Test
    @DisplayName("getPublicKeyHex() returns valid uncompressed public key")
    void testGetPublicKeyHex() {
        Secp256k1Signer signer = Secp256k1Signer.fromPrivateKey(TEST_PRIVATE_KEY);
        String pubKey = signer.getPublicKeyHex();
        assertThat(pubKey).startsWith("0x04");
        assertThat(pubKey).hasSize(2 + 130); // 0x + 65 bytes * 2 hex chars
    }

    @Test
    @DisplayName("getPrivateKeyHex() returns the original private key")
    void testGetPrivateKeyHex() {
        Secp256k1Signer signer = Secp256k1Signer.fromPrivateKey(TEST_PRIVATE_KEY);
        assertThat(signer.getPrivateKeyHex()).isEqualTo(TEST_PRIVATE_KEY);
    }

    @Test
    @DisplayName("constructor rejects null/zero/out-of-range private keys")
    void testInvalidPrivateKeys() {
        assertThatThrownBy(() -> new Secp256k1Signer(java.math.BigInteger.ZERO))
                .isInstanceOf(CryptoException.class)
                .hasMessageContaining("Invalid secp256k1 private key");

        assertThatThrownBy(() -> new Secp256k1Signer(java.math.BigInteger.valueOf(-1)))
                .isInstanceOf(CryptoException.class);

        assertThatThrownBy(() -> new Secp256k1Signer(null))
                .isInstanceOf(CryptoException.class);
    }

    @Test
    @DisplayName("EIP-2: signature enforces low-S value")
    void testLowSEnforcement() {
        Secp256k1Signer signer = Secp256k1Signer.generate();
        byte[] data = Secp256k1Signer.keccak256("eip2-test".getBytes());

        // Sign multiple times and verify S is always in the low range
        for (int i = 0; i < 10; i++) {
            SignedPayload signed = signer.sign(Secp256k1Signer.keccak256(
                    ("eip2-test-" + i).getBytes()));
            java.math.BigInteger s = new java.math.BigInteger(1,
                    java.util.Arrays.copyOfRange(signed.signature(), 32, 64));
            // S must be <= N/2 (half the curve order)
            assertThat(s.compareTo(
                    org.bouncycastle.crypto.ec.CustomNamedCurves.getByName("secp256k1")
                            .getN().shiftRight(1)))
                    .isLessThanOrEqualTo(0);
        }
    }
}
