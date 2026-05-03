package io.paladin.examples;

import io.paladin.sdk.PaladinClient;
import io.paladin.sdk.model.*;
import java.time.Duration;
import java.util.Map;

public class EventListenerExample {

    public static void main(String[] args) throws InterruptedException {
        try (var client = PaladinClient.builder()
                .endpoint("http://127.0.0.1:31548")
                .build()) {

            System.out.println("=== Paladin Java SDK — Event Listener Example ===\n");

            client.listen().onReceipt("allReceipts", receipt -> {
                System.out.printf("[RECEIPT] tx=%s domain=%s success=%s block=%d%n",
                        receipt.id(),
                        receipt.domain(),
                        receipt.success(),
                        receipt.blockNumber());
            });

            client.listen().onBlockchainEvent("allEvents", event -> {
                System.out.printf("[EVENT] block=%d sig=%s addr=%s%n",
                        event.blockNumber(),
                        event.signature(),
                        event.address());
            });

            System.out.println("Listening for events... (Ctrl+C to stop)\n");

            var txId = client.ptx().sendTransactionAsync(TransactionInput.builder()
                    .type(TransactionType.PRIVATE)
                    .domain("noto")
                    .from("owner@node1")
                    .function("mint")
                    .data(Map.of("to", "recipient@node1", "amount", 500))
                    .build())
                    .join();

            System.out.println("Submitted tx: " + txId);
            Thread.sleep(Duration.ofMinutes(5).toMillis());

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
