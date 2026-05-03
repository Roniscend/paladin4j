package io.paladin.examples;

import io.paladin.sdk.PaladinClient;
import io.paladin.sdk.model.*;
import java.time.Duration;
import java.util.Map;

public class NotoTokenExample {

    public static void main(String[] args) {
        try (var client = PaladinClient.builder()
                .endpoint("http://127.0.0.1:31548")
                .connectTimeout(Duration.ofSeconds(10))
                .requestTimeout(Duration.ofSeconds(30))
                .build()) {

            System.out.println("=== Paladin Java SDK — Noto Token Example ===\n");

            System.out.println("[1] Resolving notary key...");
            var notaryKey = client.keymgr().resolveKey("wallet", "notary", "zarith");
            System.out.println("    Verifier: " + notaryKey.verifier());

            System.out.println("\n[2] Deploying Noto token...");
            String tokenAddress = client.noto().deploy("notary@node1", Map.of(
                    "name", "USDToken",
                    "symbol", "USDT"
            ));
            System.out.println("    Contract deployed at: " + tokenAddress);

            System.out.println("\n[3] Minting 10000 tokens to recipient...");
            var mintReceipt = client.noto()
                    .mint(tokenAddress, "notary@node1", "recipient@node1", 10000)
                    .join();
            System.out.println("    Mint tx hash: " + mintReceipt.transactionHash());
            System.out.println("    Block: " + mintReceipt.blockNumber());

            System.out.println("\n[4] Transferring 3000 tokens...");
            var transferReceipt = client.noto()
                    .transfer(tokenAddress, "recipient@node1", "other@node2", 3000)
                    .join();
            System.out.println("    Transfer tx hash: " + transferReceipt.transactionHash());

            System.out.println("\n[5] Querying token states...");
            var states = client.pstate().queryContractStates(
                    "noto", tokenAddress, null,
                    QueryJSON.builder().limit(10).build(),
                    "confirmed"
            );
            System.out.println("    Found " + states.size() + " confirmed states");
            for (var state : states) {
                System.out.println("    State " + state.id() + ": " + state.data());
            }

            System.out.println("\n=== Done ===");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
