# Paladin4j 🛡️

[![Build & Test](https://github.com/Roniscend/paladin4j/actions/workflows/build.yml/badge.svg)](https://github.com/Roniscend/paladin4j/actions/workflows/build.yml)
[![Integration Tests](https://github.com/Roniscend/paladin4j/actions/workflows/integration-test.yml/badge.svg)](https://github.com/Roniscend/paladin4j/actions/workflows/integration-test.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java 17+](https://img.shields.io/badge/java-17%2B-blue.svg)](https://openjdk.org/projects/jdk/17/)

**Paladin4j** is an enterprise-grade Java SDK for interacting with [Hyperledger Paladin](https://lf-decentralized-trust-labs.github.io/paladin/) privacy nodes via JSON-RPC and WebSocket. Built natively for the JVM, it offers production-ready features like local offline signing, retry resilience, and full asynchronous support.

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Architecture](#architecture)
- [API Reference](#api-reference)
- [Domain Clients](#domain-clients)
- [Local Signing (Offline Crypto)](#local-signing-offline-crypto)
- [Event Streaming (WebSocket)](#event-streaming-websocket)
- [Configuration](#configuration)
- [Error Handling](#error-handling)
- [Examples](#examples)
- [Testing](#testing)
- [Building](#building)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Full API Coverage** — All 7 Paladin JSON-RPC namespaces (`ptx`, `pstate`, `pgroup`, `keymgr`, `bidx`, `reg`, `transport`)
- **Domain Helpers** — High-level clients for Noto (UTXO tokens), Zeto (ZK tokens), and Pente (privacy groups)
- **Local Signing** — Offline secp256k1 ECDSA signing with BouncyCastle (EIP-2 compliant)
- **Async-First** — All operations available as `CompletableFuture<T>` for non-blocking use
- **Retry & Backoff** — Configurable retry with linear backoff on 5xx errors and connection failures
- **WebSocket Events** — Real-time transaction receipts, blockchain events, and group messages
- **Zero HTTP Dependencies** — Uses JDK 17's built-in `java.net.http.HttpClient`
- **Testcontainers Support** — Docker-based integration testing against real Paladin nodes

## Prerequisites

- **Java 17** or later (tested on 17 and 21)
- **Gradle 8.x** (wrapper included)
- **Docker** (only for integration tests)
- A running [Paladin node](https://lf-decentralized-trust-labs.github.io/paladin/) (default: `http://127.0.0.1:31548`)

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.paladin:paladin-sdk-java:0.1.0-SNAPSHOT")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'io.paladin:paladin-sdk-java:0.1.0-SNAPSHOT'
}
```

### Maven

```xml
<dependency>
    <groupId>io.paladin</groupId>
    <artifactId>paladin-sdk-java</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

```java
import io.paladin.sdk.PaladinClient;
import java.util.Map;

try (var client = PaladinClient.builder()
        .endpoint("http://127.0.0.1:31548")
        .build()) {

    // Deploy a Noto privacy token
    String contractAddr = client.noto().deploy("notary@node1",
            Map.of("name", "USDToken"));

    // Mint 10,000 tokens
    var receipt = client.noto()
            .mint(contractAddr, "notary@node1", "alice@node1", 10000)
            .join();
    System.out.println("Minted at block: " + receipt.blockNumber());

    // Transfer tokens
    client.noto()
            .transfer(contractAddr, "alice@node1", "bob@node2", 3000)
            .join();
}
```

## Architecture

```
io.paladin.sdk
├── api/                   7 API namespace clients
│   ├── PtxApi             Transactions: send, get, query, prepare, waitForReceipt
│   ├── PstateApi          States: query contract states, schemas
│   ├── PgroupApi          Privacy groups: create, call, message
│   ├── KeymgrApi          Keys: resolve, reverse lookup, query
│   ├── BidxApi            Blocks: get block, query blocks/events/transactions
│   ├── RegApi             Registry: query entries
│   └── TransportApi       Peers: get local/remote peer info
├── core/                  Transport layer
│   ├── JsonRpcTransport   HTTP JSON-RPC 2.0 with retry & backoff
│   ├── WebSocketTransport WebSocket with auto-reconnection
│   ├── TransportConfig    Timeouts, retries, endpoints
│   └── PaladinException   Typed exception with RPC error codes
├── crypto/                Local signing (offline)
│   ├── LocalSigner        Interface for pluggable signing backends
│   ├── Secp256k1Signer    BouncyCastle ECDSA implementation
│   ├── SignedPayload      Signature + public key result
│   └── HexUtils           Hex encode/decode utility
├── domain/                High-level domain helpers
│   ├── NotoClient         UTXO token operations (deploy/mint/transfer/approve)
│   ├── ZetoClient         Zero-knowledge token operations
│   └── PenteClient        Privacy group EVM contract operations
├── listener/              Real-time event streaming
│   └── ListenerManager    WebSocket subscription manager
├── model/                 Java 17 Records for all Paladin types
└── PaladinClient.java     Entry point — Builder pattern, thread-safe
```

## API Reference

Every public class and method has comprehensive JavaDoc documentation. Generate the full API reference locally:

```bash
./gradlew :sdk:javadoc
# Open sdk/build/docs/javadoc/index.html
```

### Transaction Operations (PtxApi)

```java
// Send a private transaction
String txId = client.ptx().sendTransaction(TransactionInput.builder()
        .type(TransactionType.PRIVATE)
        .domain("noto")
        .from("owner@node1")
        .to(contractAddress)
        .function("transfer")
        .data(Map.of("to", "recipient@node1", "amount", 1000))
        .build());

// Wait for confirmation
TransactionReceipt receipt = client.ptx()
        .waitForReceipt(txId, Duration.ofSeconds(30))
        .join();

// Query transactions
List<TransactionFull> txns = client.ptx().queryTransactions(
        QueryJSON.builder().equal("domain", "noto").limit(10).build());
```

### State Queries (PstateApi)

```java
// Query UTXO states
List<State> states = client.pstate().queryContractStates(
        "noto", contractAddress, schemaId,
        QueryJSON.builder().limit(50).build(), "available");

// List schemas
List<Schema> schemas = client.pstate().listSchemas("noto", contractAddress);
```

### Key Management (KeymgrApi)

```java
// Resolve (or generate) a key
KeyMappingAndVerifier key = client.keymgr()
        .resolveKey("wallet", "owner1@node1", "zarith");
System.out.println("Verifier address: " + key.verifier());

// Reverse lookup
KeyMappingAndVerifier found = client.keymgr()
        .reverseKeyLookup("zarith", "0x1234...");
```

## Domain Clients

### Noto (UTXO Privacy Tokens)

```java
NotoClient noto = client.noto();

// Full lifecycle
String addr = noto.deploy("notary@node1", Map.of("name", "USD"));
noto.mint(addr, "notary@node1", "alice@node1", 10000).join();
noto.transfer(addr, "alice@node1", "bob@node2", 3000).join();
noto.approve(addr, "alice@node1", "delegate@node1", 500).join();
```

### Zeto (Zero-Knowledge Tokens)

```java
ZetoClient zeto = client.zeto();
String txId = zeto.deploy("deployer@node1", "ZKToken", true);
zeto.mint(contractAddr, "minter@node1", "recipient@node1", 1000).join();
zeto.transfer(contractAddr, "sender@node1", "receiver@node2", 500).join();
```

### Pente (Privacy Groups)

```java
PenteClient pente = client.pente();
PrivacyGroup group = pente.createGroup("myGroup", "alice@node1", "bob@node2");
pente.deployContract(group.contractAddress(), "alice@node1", abi, bytecode, params).join();
pente.invoke(group.contractAddress(), "alice@node1", "setValue", Map.of("key", "value")).join();
JsonNode result = pente.call(group.id(), Map.of("function", "getValue"));
```

## Local Signing (Offline Crypto)

For enterprise environments where private keys must never leave the client:

```java
import io.paladin.sdk.crypto.Secp256k1Signer;

// Generate a new key pair
Secp256k1Signer signer = Secp256k1Signer.generate();
System.out.println("Address: " + signer.getAddress());  // 0x1a2b...

// Or import an existing Ethereum private key
Secp256k1Signer imported = Secp256k1Signer.fromPrivateKey(
        "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80");

// Sign a transaction hash
byte[] txHash = Secp256k1Signer.keccak256(transactionBytes);
SignedPayload signed = signer.sign(txHash);

System.out.println("Signature: " + signed.signatureHex());
System.out.println("Public Key: " + signed.publicKeyHex());

// Verify
boolean valid = Secp256k1Signer.verify(txHash, signed.signature(), signed.publicKey());
```

**Features:**
- Keccak-256 hashing (Ethereum-compatible)
- EIP-2 low-S signature enforcement (prevents transaction malleability)
- Ethereum address derivation from public keys
- `LocalSigner` interface for plugging in HSMs or secure enclaves

## Event Streaming (WebSocket)

```java
// Listen for transaction receipts
client.listen().onReceipt("myListener", receipt -> {
    System.out.printf("Tx %s confirmed at block %d%n",
            receipt.id(), receipt.blockNumber());
});

// Listen for blockchain events
client.listen().onBlockchainEvent("eventWatcher", event -> {
    System.out.printf("Event %s at %s%n", event.signature(), event.address());
});

// Listen for privacy group messages
client.listen().onGroupMessage("groupListener", groupId, message -> {
    System.out.println("Message: " + message);
});

// Clean up
client.listen().removeListener("myListener");
```

## Configuration

```java
PaladinClient client = PaladinClient.builder()
        .endpoint("http://127.0.0.1:31548")         // HTTP JSON-RPC endpoint
        .wsEndpoint("ws://127.0.0.1:31549")          // WebSocket endpoint (auto-derived if omitted)
        .connectTimeout(Duration.ofSeconds(5))        // Connection timeout (default: 5s)
        .requestTimeout(Duration.ofSeconds(30))       // Per-request timeout (default: 30s)
        .maxRetries(3)                                // Max retry attempts on 5xx (default: 3)
        .build();
```

### Retry Behavior

The transport layer automatically retries on:
- **HTTP 5xx** server errors
- **IOException** (connection failures, timeouts)

It does **not** retry on:
- **HTTP 4xx** client errors (bad request, not found, etc.)
- **JSON-RPC errors** (application-level errors from the node)

Backoff is linear: `retryBackoff × (attempt + 1)` with a default base of 500ms.

## Error Handling

```java
try {
    client.ptx().sendTransaction(input);
} catch (PaladinException e) {
    if (e.isRpcError()) {
        // JSON-RPC error from the Paladin node
        System.err.println("RPC Error " + e.getRpcErrorCode() + ": " + e.getMessage());
    } else {
        // Transport/network error
        System.err.println("Transport error: " + e.getMessage());
    }
}
```

## Examples

The `examples/` module contains runnable demonstrations:

| Example | Description |
|---|---|
| `NotoTokenExample` | Deploy, mint, transfer tokens, query states |
| `EventListenerExample` | Subscribe to real-time receipts and events |
| `PrivacyGroupExample` | Create groups, deploy contracts, invoke functions |

### Running Examples

**Prerequisites:** A running Paladin node on `http://127.0.0.1:31548`

```bash
# Run the Noto token example
./gradlew :examples:run -PmainClass=io.paladin.examples.NotoTokenExample

# Run the event listener example
./gradlew :examples:run -PmainClass=io.paladin.examples.EventListenerExample
```

## Testing

### Unit Tests (fast, no Docker)

```bash
./gradlew test
# 84 tests across 16 suites — WireMock + Mockito
```

### Integration Tests (requires Docker)

```bash
./gradlew integrationTest
# Spins up a real Paladin node via Testcontainers
```

Override the Docker image:

```bash
./gradlew integrationTest -DPALADIN_IMAGE=ghcr.io/lf-decentralized-trust-labs/paladin:v0.5.0
```

### Test Coverage

```bash
./gradlew test jacocoTestReport
# Open sdk/build/reports/jacoco/test/html/index.html
```

## Building

```bash
# Full build (compile + test + javadoc + jar)
./gradlew build

# Publish to Maven Local
./gradlew :sdk:publishToMavenLocal

# Generate JavaDoc
./gradlew :sdk:javadoc
```

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Write tests for your changes
4. Ensure `./gradlew build` passes
5. Submit a pull request

## License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
