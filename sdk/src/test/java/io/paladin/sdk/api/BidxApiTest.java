package io.paladin.sdk.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.paladin.sdk.core.JsonRpcTransport;
import io.paladin.sdk.core.TransportConfig;
import io.paladin.sdk.model.*;
import org.junit.jupiter.api.*;
import java.util.List;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

class BidxApiTest {

    private static WireMockServer wireMock;
    private BidxApi bidxApi;

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
        bidxApi = new BidxApi(new JsonRpcTransport(config, mapper), mapper);
    }

    @Test
    @DisplayName("getBlockByNumber returns indexed block")
    void testGetBlockByNumber() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": {
                        "number": 42, "hash": "0xblock42", "timestamp": 1700000000,
                        "transactionCount": 5
                    }}
                    """)));
        IndexedBlock block = bidxApi.getBlockByNumber(42);
        assertThat(block.number()).isEqualTo(42);
        assertThat(block.hash()).isEqualTo("0xblock42");
        assertThat(block.transactionCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("getBlockByNumberAsync returns future with block")
    void testGetBlockByNumberAsync() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": {
                        "number": 100, "hash": "0xblock100", "timestamp": 1700000100
                    }}
                    """)));
        IndexedBlock block = bidxApi.getBlockByNumberAsync(100).join();
        assertThat(block.number()).isEqualTo(100);
    }

    @Test
    @DisplayName("getTransactionByHash returns indexed transaction")
    void testGetTransactionByHash() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": {
                        "hash": "0xtx123", "blockNumber": 42, "transactionIndex": 0,
                        "from": "0xaaa", "to": "0xbbb", "nonce": 5, "result": "success"
                    }}
                    """)));
        IndexedTransaction tx = bidxApi.getTransactionByHash("0xtx123");
        assertThat(tx.hash()).isEqualTo("0xtx123");
        assertThat(tx.blockNumber()).isEqualTo(42);
        assertThat(tx.from()).isEqualTo("0xaaa");
    }

    @Test
    @DisplayName("queryIndexedBlocks returns list of blocks")
    void testQueryIndexedBlocks() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": [
                        {"number": 1, "hash": "0xb1", "timestamp": 1000},
                        {"number": 2, "hash": "0xb2", "timestamp": 2000}
                    ]}
                    """)));
        List<IndexedBlock> blocks = bidxApi.queryIndexedBlocks(
                QueryJSON.builder().limit(10).build());
        assertThat(blocks).hasSize(2);
    }

    @Test
    @DisplayName("queryIndexedEvents returns list of events")
    void testQueryIndexedEvents() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": [
                        {"blockNumber": 42, "transactionIndex": 0, "logIndex": 0,
                         "transactionHash": "0xtx1", "signature": "Transfer(address,uint256)",
                         "address": "0xcontract"}
                    ]}
                    """)));
        List<IndexedEvent> events = bidxApi.queryIndexedEvents(
                QueryJSON.builder().limit(5).build());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).signature()).isEqualTo("Transfer(address,uint256)");
    }

    @Test
    @DisplayName("queryIndexedTransactions returns list of transactions")
    void testQueryIndexedTransactions() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": [
                        {"hash": "0xt1", "blockNumber": 10, "from": "0xa", "to": "0xb"},
                        {"hash": "0xt2", "blockNumber": 11, "from": "0xc", "to": "0xd"}
                    ]}
                    """)));
        List<IndexedTransaction> txns = bidxApi.queryIndexedTransactions(
                QueryJSON.builder().build());
        assertThat(txns).hasSize(2);
    }
}
