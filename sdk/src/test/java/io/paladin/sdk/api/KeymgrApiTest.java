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

class KeymgrApiTest {

    private static WireMockServer wireMock;
    private KeymgrApi keymgrApi;

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
        keymgrApi = new KeymgrApi(new JsonRpcTransport(config, mapper), mapper);
    }

    @Test
    @DisplayName("resolveKey returns key mapping with verifier")
    void testResolveKey() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": {
                        "verifier": "0x1234", "keyHandle": "wallet/owner1",
                        "algorithm": "zarith", "verifierType": "eth_address"
                    }}
                    """)));
        KeyMappingAndVerifier key = keymgrApi.resolveKey("wallet", "owner1@node1", "zarith");
        assertThat(key.verifier()).isEqualTo("0x1234");
        assertThat(key.algorithm()).isEqualTo("zarith");
        wireMock.verify(postRequestedFor(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", equalTo("keymgr_resolveKey"))));
    }

    @Test
    @DisplayName("resolveKeyAsync returns future with key mapping")
    void testResolveKeyAsync() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": {
                        "verifier": "0xabcd", "keyHandle": "wallet/alice",
                        "algorithm": "secp256k1", "verifierType": "eth_address"
                    }}
                    """)));
        KeyMappingAndVerifier key = keymgrApi.resolveKeyAsync("wallet", "alice@node1", "secp256k1").join();
        assertThat(key.verifier()).isEqualTo("0xabcd");
    }

    @Test
    @DisplayName("reverseKeyLookup returns key mapping from verifier")
    void testReverseKeyLookup() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": {
                        "verifier": "0x5678", "keyHandle": "wallet/bob",
                        "algorithm": "zarith", "verifierType": "eth_address"
                    }}
                    """)));
        KeyMappingAndVerifier key = keymgrApi.reverseKeyLookup("zarith", "0x5678");
        assertThat(key.keyHandle()).isEqualTo("wallet/bob");
        wireMock.verify(postRequestedFor(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", equalTo("keymgr_reverseKeyLookup"))));
    }

    @Test
    @DisplayName("queryKeys returns list of key mappings")
    void testQueryKeys() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": [
                        {"verifier": "0x1111", "keyHandle": "wallet/a", "algorithm": "zarith"},
                        {"verifier": "0x2222", "keyHandle": "wallet/b", "algorithm": "zarith"}
                    ]}
                    """)));
        QueryJSON query = QueryJSON.builder().equal("algorithm", "zarith").build();
        List<KeyMappingAndVerifier> keys = keymgrApi.queryKeys(query);
        assertThat(keys).hasSize(2);
        assertThat(keys.get(0).verifier()).isEqualTo("0x1111");
    }
}
