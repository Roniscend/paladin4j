package io.paladin.examples;

import io.paladin.sdk.PaladinClient;
import io.paladin.sdk.model.*;
import java.time.Duration;
import java.util.Map;

public class PrivacyGroupExample {

    public static void main(String[] args) {
        try (var client = PaladinClient.builder()
                .endpoint("http://127.0.0.1:31548")
                .build()) {

            System.out.println("=== Paladin Java SDK — Privacy Group Example ===\n");

            System.out.println("[1] Creating Pente privacy group...");
            var group = client.pente().createGroup("myPrivateGroup",
                    "alice@node1", "bob@node2");
            System.out.println("    Group ID: " + group.id());
            System.out.println("    Contract: " + group.contractAddress());
            System.out.println("    Members: " + String.join(", ", group.members()));

            System.out.println("\n[2] Deploying private contract...");
            var deployReceipt = client.pente().deployContract(
                    group.contractAddress(),
                    "alice@node1",
                    null,
                    null,
                    Map.of("initialValue", 42)
            ).join();
            System.out.println("    Deploy tx: " + deployReceipt.transactionHash());

            System.out.println("\n[3] Invoking 'setValue' on private contract...");
            var invokeReceipt = client.pente().invoke(
                    group.contractAddress(),
                    "alice@node1",
                    "setValue",
                    Map.of("newValue", 100)
            ).join();
            System.out.println("    Invoke tx: " + invokeReceipt.transactionHash());

            System.out.println("\n[4] Calling 'getValue' (read-only)...");
            var result = client.pente().call(group.id(), Map.of(
                    "function", "getValue"
            ));
            System.out.println("    Value: " + result);

            System.out.println("\n=== Done ===");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
