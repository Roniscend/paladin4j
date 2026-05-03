package io.paladin.sdk.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.paladin.sdk.core.JsonRpcTransport;
import io.paladin.sdk.core.PaladinException;
import io.paladin.sdk.core.TransportConfig;
import io.paladin.sdk.model.*;
import org.junit.jupiter.api.*;
import java.time.Duration;
import java.util.List;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

class PtxApiTest {

    private static WireMockServer wireMock;
    private PtxApi ptxApi;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(0);
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        ObjectMapper mapper = new ObjectMapper();
        TransportConfig config = TransportConfig.builder()
                .httpEndpoint("http://localhost:" + wireMock.port())
                .maxRetries(0)
                .build();
        ptxApi = new PtxApi(new JsonRpcTransport(config, mapper), mapper);
    }

    @Test
    @DisplayName("sendTransaction returns transaction ID")
    void testSendTransaction() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": "tx-12345"}
                    """)));
        TransactionInput input = TransactionInput.builder()
                .type(TransactionType.PRIVATE)
                .domain("noto")
                .from("owner@node1")
                .function("mint")
                .data(java.util.Map.of("amount", 1000))
                .build();
        String txId = ptxApi.sendTransaction(input);
        assertThat(txId).isEqualTo("tx-12345");
        wireMock.verify(postRequestedFor(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", equalTo("ptx_sendTransaction"))));
    }

    @Test
    @DisplayName("sendTransactionAsync returns future with transaction ID")
    void testSendTransactionAsync() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": "tx-async-001"}
                    """)));
        TransactionInput input = TransactionInput.builder()
                .type(TransactionType.PRIVATE)
                .domain("noto")
                .from("owner@node1")
                .function("transfer")
                .build();
        String txId = ptxApi.sendTransactionAsync(input).join();
        assertThat(txId).isEqualTo("tx-async-001");
    }

    @Test
    @DisplayName("getTransaction returns full transaction details")
    void testGetTransaction() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": {
                        "id": "tx-001", "type": "private", "domain": "noto",
                        "from": "owner@node1", "function": "mint"
                    }}
                    """)));
        TransactionFull tx = ptxApi.getTransaction("tx-001");
        assertThat(tx.id()).isEqualTo("tx-001");
        assertThat(tx.domain()).isEqualTo("noto");
        assertThat(tx.from()).isEqualTo("owner@node1");
    }

    @Test
    @DisplayName("getTransactionReceipt returns receipt")
    void testGetTransactionReceipt() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": {
                        "id": "tx-001", "success": true, "blockNumber": 42,
                        "transactionHash": "0xabc", "domain": "noto",
                        "contractAddress": "0xdef"
                    }}
                    """)));
        TransactionReceipt receipt = ptxApi.getTransactionReceipt("tx-001");
        assertThat(receipt.id()).isEqualTo("tx-001");
        assertThat(receipt.success()).isTrue();
        assertThat(receipt.blockNumber()).isEqualTo(42);
        assertThat(receipt.contractAddress()).isEqualTo("0xdef");
    }

    @Test
    @DisplayName("prepareTransaction returns prepared payload")
    void testPrepareTransaction() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": {
                        "id": "prep-001", "domain": "noto", "to": "0x123"
                    }}
                    """)));
        TransactionInput input = TransactionInput.builder()
                .type(TransactionType.PRIVATE)
                .domain("noto")
                .from("owner@node1")
                .function("transfer")
                .build();
        PreparedTransaction prepared = ptxApi.prepareTransaction(input);
        assertThat(prepared.id()).isEqualTo("prep-001");
        assertThat(prepared.domain()).isEqualTo("noto");
    }

    @Test
    @DisplayName("queryTransactions returns list of transactions")
    void testQueryTransactions() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": [
                        {"id": "tx-001", "type": "private", "domain": "noto", "from": "a@n1"},
                        {"id": "tx-002", "type": "private", "domain": "noto", "from": "b@n1"}
                    ]}
                    """)));
        QueryJSON query = QueryJSON.builder().equal("domain", "noto").limit(10).build();
        List<TransactionFull> txns = ptxApi.queryTransactions(query);
        assertThat(txns).hasSize(2);
        assertThat(txns.get(0).id()).isEqualTo("tx-001");
        assertThat(txns.get(1).id()).isEqualTo("tx-002");
    }

    @Test
    @DisplayName("waitForReceipt polls and returns receipt when available")
    void testWaitForReceipt() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": {
                        "id": "tx-001", "success": true, "blockNumber": 10
                    }}
                    """)));
        TransactionReceipt receipt = ptxApi.waitForReceipt("tx-001", Duration.ofSeconds(5)).join();
        assertThat(receipt).isNotNull();
        assertThat(receipt.success()).isTrue();
    }
}
