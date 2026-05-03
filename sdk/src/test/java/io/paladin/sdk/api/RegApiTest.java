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

class RegApiTest {

    private static WireMockServer wireMock;
    private RegApi regApi;

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
        regApi = new RegApi(new JsonRpcTransport(config, mapper), mapper);
    }

    @Test
    @DisplayName("getRegistryEntries returns list of entries")
    void testGetRegistryEntries() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": [
                        {"id": "entry-1", "registry": "domains", "name": "noto"},
                        {"id": "entry-2", "registry": "domains", "name": "zeto"}
                    ]}
                    """)));
        QueryJSON query = QueryJSON.builder().limit(10).build();
        List<RegistryEntry> entries = regApi.getRegistryEntries("domains", query);
        assertThat(entries).hasSize(2);
        wireMock.verify(postRequestedFor(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", equalTo("reg_getRegistryEntries"))));
    }

    @Test
    @DisplayName("getRegistryEntryByID returns specific entry")
    void testGetRegistryEntryByID() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": {
                        "id": "entry-1", "registry": "domains", "name": "noto"
                    }}
                    """)));
        RegistryEntry entry = regApi.getRegistryEntryByID("domains", "entry-1");
        assertThat(entry.id()).isEqualTo("entry-1");
    }
}
