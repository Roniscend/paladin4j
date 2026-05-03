package io.paladin.examples;

import io.paladin.sdk.PaladinClient;
import io.paladin.sdk.crypto.Secp256k1Signer;
import io.paladin.sdk.crypto.SignedPayload;
import io.paladin.sdk.model.TransactionInput;
import io.paladin.sdk.model.TransactionType;

import java.util.Map;

/**
 * Example demonstrating how to use the local secp256k1 signer for offline transactions.
 * In a real enterprise application, private keys would be securely loaded from a vault,
 * HSM, or secure enclave, rather than hardcoded.
 */
public class LocalSigningExample {

    private static final String TEST_PRIVATE_KEY = "ac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80";

    public static void main(String[] args) {
        System.out.println("=== Paladin Java SDK — Local Signing Example ===\n");

        System.out.println("[1] Initializing offline signer...");
        Secp256k1Signer signer = Secp256k1Signer.fromPrivateKey(TEST_PRIVATE_KEY);
        System.out.println("    Derived Address: " + signer.getAddress());

        try (var client = PaladinClient.builder()
                .endpoint("http://127.0.0.1:31548")
                .signer(signer)
                .build()) {

            System.out.println("\n[2] Demonstrating offline signing...");
            String message = "Hello Paladin";
            byte[] txHash = Secp256k1Signer.keccak256(message.getBytes());
            System.out.println("    Payload Hash:  0x" + io.paladin.sdk.crypto.HexUtils.toHex(txHash));

            SignedPayload signature = signer.sign(txHash);
            System.out.println("    Signature:     " + signature.signatureHex());
            System.out.println("    Public Key:    " + signature.publicKeyHex());

            System.out.println("    Verify (local): " + Secp256k1Signer.verify(txHash, signature.signature(), signature.publicKey()));

            System.out.println("\n[3] Preparing transaction for node...");
            var prepared = client.ptx().prepareTransaction(TransactionInput.builder()
                    .type(TransactionType.PRIVATE)
                    .domain("noto")
                    .from(signer.getAddress())
                    .function("transfer")
                    .data(Map.of("to", "recipient@node1", "amount", 100))
                    .build());
            
            System.out.println("    Prepared TX ID: " + prepared.id());
            System.out.println("    (In a full flow, you would sign prepared.metadata() and submit)");

            System.out.println("\n=== Done ===");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
