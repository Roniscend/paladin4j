# Paladin Java SDK Examples

This module contains runnable examples demonstrating how to use the Paladin Java SDK to interact with a Paladin privacy node.

## Prerequisites

To run these examples, you need:
1. **Java 17+**
2. A running **Paladin node** accessible at `http://127.0.0.1:31548`. You can start a local Paladin node using the official [Paladin Quickstart](https://lf-decentralized-trust-labs.github.io/paladin/).

## Available Examples

| Example | Description |
|---------|-------------|
| `NotoTokenExample` | Deploys a Noto (UTXO) token, mints tokens, transfers them, and queries the UTXO states. |
| `PrivacyGroupExample` | Creates a Pente privacy group, deploys an EVM contract to the group, and invokes it. |
| `EventListenerExample` | Subscribes to real-time WebSocket events for transaction receipts and blockchain events. |
| `LocalSigningExample` | Demonstrates configuring the SDK with an offline secp256k1 signer for local payload signing. |

## How to Run

You can run any example from the repository root using the Gradle `run` task and passing the `mainClass` property.

```bash
# Run Noto Token Example
./gradlew :examples:run -PmainClass=io.paladin.examples.NotoTokenExample

# Run Privacy Group Example
./gradlew :examples:run -PmainClass=io.paladin.examples.PrivacyGroupExample

# Run Event Listener Example
./gradlew :examples:run -PmainClass=io.paladin.examples.EventListenerExample

# Run Local Signing Example
./gradlew :examples:run -PmainClass=io.paladin.examples.LocalSigningExample
```
