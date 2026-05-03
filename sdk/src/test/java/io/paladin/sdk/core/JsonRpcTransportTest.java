package io.paladin.sdk.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.*;
import java.time.Duration;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

class JsonRpcTransportTest {

    private static WireMockServer wireMock;
    private JsonRpcTransport transport;
    private ObjectMapper mapper;

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
        mapper = new ObjectMapper();
        TransportConfig config = TransportConfig.builder()
                .httpEndpoint("http://localhost:" + wireMock.port())
                .connectTimeout(Duration.ofSeconds(2))
                .requestTimeout(Duration.ofSeconds(5))
                .maxRetries(0)
                .build();
        transport = new JsonRpcTransport(config, mapper);
    }

    @Test
    @DisplayName("Successful JSON-RPC call deserializes result")
    void testSuccessfulInvoke() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {
                      "jsonrpc": "2.0",
                      "id": 1,
                      "result": "0xabc123"
                    }
                    """)));
        String result = transport.invoke("ptx_sendTransaction", String.class, "param1");
        assertThat(result).isEqualTo("0xabc123");
        wireMock.verify(postRequestedFor(urlEqualTo("/"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(matchingJsonPath("$.method", equalTo("ptx_sendTransaction")))
                .withRequestBody(matchingJsonPath("$.jsonrpc", equalTo("2.0"))));
    }

    @Test
    @DisplayName("JSON-RPC error response throws PaladinException")
    void testRpcErrorResponse() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {
                      "jsonrpc": "2.0",
                      "id": 1,
                      "error": {
                        "code": -32601,
                        "message": "Method not found"
                      }
                    }
                    """)));
        assertThatThrownBy(() -> transport.invoke("unknown_method", String.class))
                .isInstanceOf(PaladinException.class)
                .hasMessageContaining("Method not found");
    }

    @Test
    @DisplayName("HTTP 500 without retry throws PaladinException")
    void testServerErrorNoRetry() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(serverError().withBody("Internal Server Error")));
        assertThatThrownBy(() -> transport.invoke("ptx_getTransaction", String.class, "txId"))
                .isInstanceOf(PaladinException.class)
                .hasMessageContaining("HTTP 500");
    }

    @Test
    @DisplayName("Async invocation completes successfully")
    void testAsyncInvoke() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {
                      "jsonrpc": "2.0",
                      "id": 1,
                      "result": {"id": "tx-001", "success": true, "blockNumber": 42}
                    }
                    """)));
        var future = transport.invokeAsync("ptx_getTransactionReceipt",
                io.paladin.sdk.model.TransactionReceipt.class, "tx-001");
        var receipt = future.join();
        assertThat(receipt).isNotNull();
        assertThat(receipt.id()).isEqualTo("tx-001");
        assertThat(receipt.success()).isTrue();
        assertThat(receipt.blockNumber()).isEqualTo(42);
    }

    @Test
    @DisplayName("Retry on HTTP 500 when maxRetries > 0")
    void testRetryOnServerError() {
        TransportConfig retryConfig = TransportConfig.builder()
                .httpEndpoint("http://localhost:" + wireMock.port())
                .maxRetries(2)
                .retryBackoff(Duration.ofMillis(100))
                .build();
        JsonRpcTransport retryTransport = new JsonRpcTransport(retryConfig, mapper);
        wireMock.stubFor(post(urlEqualTo("/"))
                .inScenario("retry")
                .whenScenarioStateIs("Started")
                .willReturn(serverError())
                .willSetStateTo("firstRetry"));
        wireMock.stubFor(post(urlEqualTo("/"))
                .inScenario("retry")
                .whenScenarioStateIs("firstRetry")
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": "recovered"}
                    """)));
        String result = retryTransport.invoke("ptx_test", String.class);
        assertThat(result).isEqualTo("recovered");
    }
}
