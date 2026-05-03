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

class PstateApiTest {

    private static WireMockServer wireMock;
    private PstateApi pstateApi;

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
        pstateApi = new PstateApi(new JsonRpcTransport(config, mapper), mapper);
    }

    @Test
    @DisplayName("queryContractStates returns list of states")
    void testQueryContractStates() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": [
                        {"id": "state-1", "schemaId": "schema-1", "domainName": "noto"}
                    ]}
                    """)));
        QueryJSON query = QueryJSON.builder().limit(10).build();
        List<State> states = pstateApi.queryContractStates("noto", "0x123", "schema-1", query, "available");
        assertThat(states).hasSize(1);
        wireMock.verify(postRequestedFor(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", equalTo("pstate_queryContractStates"))));
    }

    @Test
    @DisplayName("queryContractStatesAsync returns future with states")
    void testQueryContractStatesAsync() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": [
                        {"id": "state-1"},
                        {"id": "state-2"}
                    ]}
                    """)));
        QueryJSON query = QueryJSON.builder().limit(20).build();
        List<State> states = pstateApi.queryContractStatesAsync("noto", "0x123", "schema-1", query, "all").join();
        assertThat(states).hasSize(2);
    }

    @Test
    @DisplayName("getSchema returns schema definition")
    void testGetSchema() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": {
                        "id": "schema-1", "domainName": "noto", "type": "abi"
                    }}
                    """)));
        Schema schema = pstateApi.getSchema("noto", "schema-1");
        assertThat(schema.id()).isEqualTo("schema-1");
    }

    @Test
    @DisplayName("listSchemas returns list of schemas")
    void testListSchemas() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": [
                        {"id": "schema-1", "domainName": "noto", "type": "abi"},
                        {"id": "schema-2", "domainName": "noto", "type": "abi"}
                    ]}
                    """)));
        List<Schema> schemas = pstateApi.listSchemas("noto", "0x123");
        assertThat(schemas).hasSize(2);
    }
}
