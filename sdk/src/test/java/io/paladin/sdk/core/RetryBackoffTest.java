package io.paladin.sdk.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import java.time.Duration;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for the retry and exponential backoff logic in {@link JsonRpcTransport}.
 */
class RetryBackoffTest {

    private static WireMockServer wireMock;
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
    }

    @Test
    @DisplayName("Retries on HTTP 503 and succeeds on second attempt")
    void testRetryOn503() {
        TransportConfig config = TransportConfig.builder()
                .httpEndpoint("http://localhost:" + wireMock.port())
                .maxRetries(3)
                .retryBackoff(Duration.ofMillis(50))
                .build();
        JsonRpcTransport transport = new JsonRpcTransport(config, mapper);

        wireMock.stubFor(post(urlEqualTo("/"))
                .inScenario("retry503")
                .whenScenarioStateIs("Started")
                .willReturn(serviceUnavailable().withBody("Service Unavailable"))
                .willSetStateTo("retried"));
        wireMock.stubFor(post(urlEqualTo("/"))
                .inScenario("retry503")
                .whenScenarioStateIs("retried")
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": "recovered"}
                    """)));

        String result = transport.invoke("test_method", String.class);
        assertThat(result).isEqualTo("recovered");
        wireMock.verify(2, postRequestedFor(urlEqualTo("/")));
    }

    @Test
    @DisplayName("Exhausts all retries and throws PaladinException")
    void testRetryExhaustion() {
        TransportConfig config = TransportConfig.builder()
                .httpEndpoint("http://localhost:" + wireMock.port())
                .maxRetries(2)
                .retryBackoff(Duration.ofMillis(50))
                .build();
        JsonRpcTransport transport = new JsonRpcTransport(config, mapper);

        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(serverError().withBody("Persistent failure")));

        assertThatThrownBy(() -> transport.invoke("test_method", String.class))
                .isInstanceOf(PaladinException.class)
                .hasMessageContaining("HTTP 500");
        // 1 original + 2 retries = 3 total calls
        wireMock.verify(3, postRequestedFor(urlEqualTo("/")));
    }

    @Test
    @DisplayName("Backoff delay increases linearly with each retry")
    void testBackoffTiming() {
        TransportConfig config = TransportConfig.builder()
                .httpEndpoint("http://localhost:" + wireMock.port())
                .maxRetries(2)
                .retryBackoff(Duration.ofMillis(100))
                .build();
        JsonRpcTransport transport = new JsonRpcTransport(config, mapper);

        wireMock.stubFor(post(urlEqualTo("/"))
                .inScenario("timing")
                .whenScenarioStateIs("Started")
                .willReturn(serverError())
                .willSetStateTo("attempt2"));
        wireMock.stubFor(post(urlEqualTo("/"))
                .inScenario("timing")
                .whenScenarioStateIs("attempt2")
                .willReturn(serverError())
                .willSetStateTo("attempt3"));
        wireMock.stubFor(post(urlEqualTo("/"))
                .inScenario("timing")
                .whenScenarioStateIs("attempt3")
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": "finally"}
                    """)));

        long start = System.currentTimeMillis();
        String result = transport.invoke("test_method", String.class);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(result).isEqualTo("finally");
        // Backoff: 100ms + 200ms = 300ms minimum
        assertThat(elapsed).isGreaterThanOrEqualTo(250);
    }

    @Test
    @DisplayName("No retry on HTTP 400 (client error)")
    void testNoRetryOnClientError() {
        TransportConfig config = TransportConfig.builder()
                .httpEndpoint("http://localhost:" + wireMock.port())
                .maxRetries(3)
                .retryBackoff(Duration.ofMillis(50))
                .build();
        JsonRpcTransport transport = new JsonRpcTransport(config, mapper);

        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse().withStatus(400).withBody("Bad Request")));

        assertThatThrownBy(() -> transport.invoke("test_method", String.class))
                .isInstanceOf(PaladinException.class)
                .hasMessageContaining("HTTP 400");
        // Only 1 attempt — no retries for 4xx
        wireMock.verify(1, postRequestedFor(urlEqualTo("/")));
    }

    @Test
    @DisplayName("TransportConfig validates maxRetries >= 0")
    void testInvalidRetryConfig() {
        assertThatThrownBy(() -> TransportConfig.builder().maxRetries(-1).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxRetries");
    }

    @Test
    @DisplayName("TransportConfig default backoff is 500ms")
    void testDefaultBackoff() {
        TransportConfig config = TransportConfig.builder().build();
        assertThat(config.retryBackoff()).isEqualTo(Duration.ofMillis(500));
        assertThat(config.maxRetries()).isEqualTo(3);
    }
}
