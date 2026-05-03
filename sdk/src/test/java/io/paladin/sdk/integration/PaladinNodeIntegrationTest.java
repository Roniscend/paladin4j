package io.paladin.sdk.integration;

import io.paladin.sdk.PaladinClient;
import io.paladin.sdk.model.*;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests that spin up a real Paladin Docker container via Testcontainers
 * and exercise the SDK end-to-end.
 *
 * <p>These tests are tagged with {@code "integration"} and are excluded from the
 * default {@code ./gradlew test} run. Execute them with:
 * <pre>{@code
 * ./gradlew integrationTest
 * }</pre>
 *
 * <p><strong>Prerequisites:</strong> Docker must be running on the host machine.
 * The Paladin image will be pulled automatically on first run.
 * If Docker is unavailable or the container fails to start, all tests are skipped.
 */
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaladinNodeIntegrationTest {

    private static PaladinContainer paladin;
    private static PaladinClient client;

    @BeforeAll
    static void setup() {
        paladin = new PaladinContainer();
        try {
            paladin.start();
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                    "Paladin container failed to start (skipping integration tests): " + e.getMessage());
            return;
        }
        client = PaladinClient.builder()
                .endpoint(paladin.getHttpEndpoint())
                .wsEndpoint(paladin.getWsEndpoint())
                .connectTimeout(Duration.ofSeconds(10))
                .requestTimeout(Duration.ofSeconds(30))
                .maxRetries(5)
                .build();
        // Probe the actual JSON-RPC API — skip rather than fail if the node isn't ready
        try {
            client.transport().getLocalPeerInfo();
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                    "Paladin JSON-RPC API not reachable (skipping integration tests): " + e.getMessage());
        }
    }

    @AfterAll
    static void cleanup() {
        if (client != null) client.close();
        if (paladin != null && paladin.isRunning()) paladin.stop();
    }

    // ──────────────────────────────────────────────────────────────
    // Transport Layer Tests — Verify we can talk to the node
    // ──────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Node is reachable and responds to transport_getLocalPeerInfo")
    void testNodeIsReachable() {
        var peerInfo = client.transport().getLocalPeerInfo();
        assertThat(peerInfo).isNotNull();
        assertThat(peerInfo.id()).isNotBlank();
    }

    // ──────────────────────────────────────────────────────────────
    // Key Management Tests — Verify key resolution
    // ──────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("keymgr can resolve a new key on-the-fly")
    void testResolveKey() {
        var keyMapping = client.keymgr().resolveKey("wallet", "testOwner@node1", "zarith");
        assertThat(keyMapping).isNotNull();
        assertThat(keyMapping.verifier()).isNotBlank();
        assertThat(keyMapping.algorithm()).isEqualTo("zarith");
    }

    @Test
    @Order(3)
    @DisplayName("keymgr reverseKeyLookup returns the same mapping")
    void testReverseKeyLookup() {
        var resolved = client.keymgr().resolveKey("wallet", "reverseLookupUser@node1", "zarith");
        var reversed = client.keymgr().reverseKeyLookup("zarith", resolved.verifier());
        assertThat(reversed.verifier()).isEqualTo(resolved.verifier());
    }

    // ──────────────────────────────────────────────────────────────
    // Block Indexer Tests — Verify chain data access
    // ──────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("bidx can query the genesis block (block 0)")
    void testGetGenesisBlock() {
        var block = client.bidx().getBlockByNumber(0);
        assertThat(block).isNotNull();
        assertThat(block.number()).isEqualTo(0);
        assertThat(block.hash()).isNotBlank();
    }

    @Test
    @Order(5)
    @DisplayName("bidx queryIndexedBlocks returns at least genesis")
    void testQueryBlocks() {
        var blocks = client.bidx().queryIndexedBlocks(
                QueryJSON.builder().limit(5).build());
        assertThat(blocks).isNotEmpty();
    }

    // ──────────────────────────────────────────────────────────────
    // Noto Domain Tests — End-to-end token lifecycle
    // ──────────────────────────────────────────────────────────────

    private static String notoContractAddress;

    @Test
    @Order(10)
    @DisplayName("Noto: deploy a new token contract")
    void testNotoDeploy() {
        notoContractAddress = client.noto().deploy(
                "notary@node1",
                Map.of("name", "IntegrationTestToken")
        );
        assertThat(notoContractAddress).isNotBlank().startsWith("0x");
    }

    @Test
    @Order(11)
    @DisplayName("Noto: mint tokens to a recipient")
    void testNotoMint() {
        assertThat(notoContractAddress).as("deploy must run first").isNotNull();
        var receipt = client.noto().mint(
                notoContractAddress, "notary@node1", "recipient@node1", 10000
        ).join();
        assertThat(receipt).isNotNull();
        assertThat(receipt.success()).isTrue();
        assertThat(receipt.blockNumber()).isGreaterThan(0);
    }

    @Test
    @Order(12)
    @DisplayName("Noto: transfer tokens between recipients")
    void testNotoTransfer() {
        assertThat(notoContractAddress).as("deploy must run first").isNotNull();
        var receipt = client.noto().transfer(
                notoContractAddress, "recipient@node1", "other@node1", 3000
        ).join();
        assertThat(receipt).isNotNull();
        assertThat(receipt.success()).isTrue();
    }

    @Test
    @Order(13)
    @DisplayName("Noto: query UTXO states after mint+transfer")
    void testNotoQueryStates() {
        assertThat(notoContractAddress).as("deploy must run first").isNotNull();
        var schemas = client.pstate().listSchemas("noto", notoContractAddress);
        assertThat(schemas).isNotEmpty();

        String schemaId = schemas.stream()
                .filter(s -> "state".equals(s.type()))
                .map(Schema::id)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No state schema found for Noto contract"));
        var states = client.pstate().queryContractStates(
                "noto", notoContractAddress, schemaId,
                QueryJSON.builder().limit(50).build(), "available"
        );
        assertThat(states).isNotEmpty();
    }

    // ──────────────────────────────────────────────────────────────
    // Transaction Query Tests — Verify ptx namespace
    // ──────────────────────────────────────────────────────────────

    @Test
    @Order(20)
    @DisplayName("ptx queryTransactions returns results")
    void testQueryTransactions() {
        var txns = client.ptx().queryTransactions(
                QueryJSON.builder().equal("domain", "noto").limit(10).build());
        assertThat(txns).isNotEmpty();
    }

    @Test
    @Order(21)
    @DisplayName("ptx sendTransaction + getTransaction round-trip")
    void testSendAndGetTransaction() {
        // Resolve a key first for the notary field
        var key = client.keymgr().resolveKey("wallet", "roundtripNotary@node1", "zarith");
        String txId = client.ptx().sendTransaction(TransactionInput.builder()
                .type(TransactionType.PRIVATE)
                .domain("noto")
                .from("roundtripNotary@node1")
                .function("constructor")
                .data(Map.of("notary", key.verifier(), "name", "RoundtripToken"))
                .build());
        assertThat(txId).isNotBlank();

        var receipt = client.ptx().waitForReceipt(txId, Duration.ofSeconds(30)).join();
        assertThat(receipt).isNotNull();

        var txFull = client.ptx().getTransaction(txId);
        assertThat(txFull).isNotNull();
        assertThat(txFull.id()).isEqualTo(txId);
        assertThat(txFull.domain()).isEqualTo("noto");
    }

    // ──────────────────────────────────────────────────────────────
    // Registry Tests — Verify on-chain registry access
    // ──────────────────────────────────────────────────────────────

    @Test
    @Order(30)
    @DisplayName("reg queryRegistryEntries returns entries")
    void testQueryRegistry() {
        // The "domains" registry is always populated after Paladin starts, 
        // but it might take a few seconds to sync.
        List<RegistryEntry> entries = null;
        for (int i = 0; i < 10; i++) {
            entries = client.reg().getRegistryEntries("domains",
                    QueryJSON.builder().limit(10).build());
            if (!entries.isEmpty()) break;
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }
        assertThat(entries).as("domains registry should not be empty").isNotEmpty();
    }
}
