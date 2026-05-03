package io.paladin.sdk.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.paladin.sdk.core.JsonRpcTransport;
import io.paladin.sdk.core.TransportConfig;
import io.paladin.sdk.model.*;
import org.junit.jupiter.api.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

class TransportApiTest {

    private static WireMockServer wireMock;
    private TransportApi transportApi;

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
        transportApi = new TransportApi(new JsonRpcTransport(config, mapper));
    }

    @Test
    @DisplayName("getLocalPeerInfo returns local node peer info")
    void testGetLocalPeerInfo() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": {
                        "id": "peer-local", "name": "node1",
                        "endpoint": "https://node1.example.com", "connected": true
                    }}
                    """)));
        PeerInfo info = transportApi.getLocalPeerInfo();
        assertThat(info.id()).isEqualTo("peer-local");
        assertThat(info.name()).isEqualTo("node1");
        assertThat(info.connected()).isTrue();
        wireMock.verify(postRequestedFor(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", equalTo("transport_getLocalPeerInfo"))));
    }

    @Test
    @DisplayName("getPeerInfo returns remote peer info")
    void testGetPeerInfo() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": {
                        "id": "peer-remote", "name": "node2",
                        "endpoint": "https://node2.example.com", "connected": false
                    }}
                    """)));
        PeerInfo info = transportApi.getPeerInfo("peer-remote");
        assertThat(info.id()).isEqualTo("peer-remote");
        assertThat(info.connected()).isFalse();
    }

    @Test
    @DisplayName("getLocalPeerInfoAsync returns future with peer info")
    void testGetLocalPeerInfoAsync() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": {
                        "id": "peer-local", "name": "node1",
                        "endpoint": "https://node1.example.com", "connected": true
                    }}
                    """)));
        PeerInfo info = transportApi.getLocalPeerInfoAsync().join();
        assertThat(info).isNotNull();
        assertThat(info.id()).isEqualTo("peer-local");
    }
}
