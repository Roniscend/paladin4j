package io.paladin.sdk.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.paladin.sdk.core.JsonRpcTransport;
import io.paladin.sdk.core.TransportConfig;
import io.paladin.sdk.model.*;
import org.junit.jupiter.api.*;
import java.util.Map;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

class PgroupApiTest {

    private static WireMockServer wireMock;
    private PgroupApi pgroupApi;

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
        pgroupApi = new PgroupApi(new JsonRpcTransport(config, mapper), mapper);
    }

    @Test
    @DisplayName("createGroup returns the created privacy group")
    void testCreateGroup() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": {
                        "id": "group-001", "domain": "pente", "name": "myGroup",
                        "members": ["alice@node1", "bob@node2"],
                        "contractAddress": "0xabc"
                    }}
                    """)));
        PrivacyGroupInput input = PrivacyGroupInput.pente("myGroup", "alice@node1", "bob@node2");
        PrivacyGroup group = pgroupApi.createGroup(input);
        assertThat(group.id()).isEqualTo("group-001");
        assertThat(group.name()).isEqualTo("myGroup");
        assertThat(group.members()).containsExactly("alice@node1", "bob@node2");
        wireMock.verify(postRequestedFor(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", equalTo("pgroup_create"))));
    }

    @Test
    @DisplayName("createGroupAsync returns future with privacy group")
    void testCreateGroupAsync() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": {
                        "id": "group-002", "domain": "pente", "name": "asyncGroup"
                    }}
                    """)));
        PrivacyGroup group = pgroupApi.createGroupAsync(
                PrivacyGroupInput.pente("asyncGroup", "alice@node1")).join();
        assertThat(group.id()).isEqualTo("group-002");
    }

    @Test
    @DisplayName("call returns JSON result from privacy group")
    void testCall() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": {"balance": 5000}}
                    """)));
        var result = pgroupApi.call("group-001", Map.of("method", "getBalance"));
        assertThat(result.get("balance").asInt()).isEqualTo(5000);
    }

    @Test
    @DisplayName("sendMessage returns message ID")
    void testSendMessage() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": "msg-001"}
                    """)));
        String msgId = pgroupApi.sendMessage("group-001", Map.of("payload", "hello"));
        assertThat(msgId).isEqualTo("msg-001");
    }

    @Test
    @DisplayName("getGroup returns privacy group details")
    void testGetGroup() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson("""
                    {"jsonrpc": "2.0", "id": 1, "result": {
                        "id": "group-001", "domain": "pente", "name": "testGroup",
                        "contractAddress": "0xdef"
                    }}
                    """)));
        PrivacyGroup group = pgroupApi.getGroup("group-001");
        assertThat(group.id()).isEqualTo("group-001");
        assertThat(group.contractAddress()).isEqualTo("0xdef");
    }
}
