package io.paladin.sdk.model;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
class ModelSerializationTest {
    private final ObjectMapper mapper = new ObjectMapper();
    @Test
    @DisplayName("TransactionInput serializes with builder pattern")
    void testTransactionInputSerialization() throws Exception {
        TransactionInput input = TransactionInput.builder()
                .type(TransactionType.PRIVATE)
                .domain("noto")
                .from("owner@node1")
                .function("mint")
                .data(java.util.Map.of("amount", 1000))
                .build();
        String json = mapper.writeValueAsString(input);
        assertThat(json).contains("\"type\":\"private\"");
        assertThat(json).contains("\"domain\":\"noto\"");
        assertThat(json).contains("\"from\":\"owner@node1\"");
        assertThat(json).contains("\"function\":\"mint\"");
    }
    @Test
    @DisplayName("TransactionReceipt deserializes from JSON")
    void testTransactionReceiptDeserialization() throws Exception {
        String json = """
            {
              "id": "tx-001",
              "success": true,
              "transactionHash": "0xabc123",
              "blockNumber": 42,
              "domain": "noto",
              "contractAddress": "0xdef456"
            }
            """;
        TransactionReceipt receipt = mapper.readValue(json, TransactionReceipt.class);
        assertThat(receipt.id()).isEqualTo("tx-001");
        assertThat(receipt.success()).isTrue();
        assertThat(receipt.blockNumber()).isEqualTo(42);
        assertThat(receipt.domain()).isEqualTo("noto");
        assertThat(receipt.contractAddress()).isEqualTo("0xdef456");
        assertThat(receipt.isFailed()).isFalse();
    }
    @Test
    @DisplayName("QueryJSON builds with fluent API")
    void testQueryJsonBuilder() throws Exception {
        QueryJSON query = QueryJSON.builder()
                .equal("domain", "noto")
                .greaterThan("blockNumber", 100)
                .sortDesc("blockNumber")
                .limit(10)
                .build();
        String json = mapper.writeValueAsString(query);
        assertThat(json).contains("\"domain\"");
        assertThat(json).contains("\"noto\"");
        assertThat(json).contains("\"limit\":10");
    }
    @Test
    @DisplayName("PrivacyGroupInput factory creates Pente group")
    void testPenteGroupFactory() throws Exception {
        PrivacyGroupInput input = PrivacyGroupInput.pente("myGroup",
                "member1@node1", "member2@node2");
        assertThat(input.domain()).isEqualTo("pente");
        assertThat(input.name()).isEqualTo("myGroup");
        assertThat(input.members()).containsExactly("member1@node1", "member2@node2");
        String json = mapper.writeValueAsString(input);
        assertThat(json).contains("\"domain\":\"pente\"");
    }
    @Test
    @DisplayName("KeyMappingAndVerifier round-trips through JSON")
    void testKeyMappingRoundTrip() throws Exception {
        String json = """
            {
              "verifier": "0x1234567890abcdef",
              "keyHandle": "wallet/owner1",
              "algorithm": "zarith",
              "verifierType": "eth_address"
            }
            """;
        KeyMappingAndVerifier key = mapper.readValue(json, KeyMappingAndVerifier.class);
        assertThat(key.verifier()).isEqualTo("0x1234567890abcdef");
        assertThat(key.algorithm()).isEqualTo("zarith");
        String reJson = mapper.writeValueAsString(key);
        assertThat(reJson).contains("0x1234567890abcdef");
    }
    @Test
    @DisplayName("Unknown JSON fields are ignored during deserialization")
    void testUnknownFieldsIgnored() throws Exception {
        String json = """
            {
              "id": "peer-1",
              "name": "node1",
              "endpoint": "https://node1.example.com",
              "connected": true,
              "unknownField": "should be ignored"
            }
            """;
        PeerInfo peer = mapper.readValue(json, PeerInfo.class);
        assertThat(peer.id()).isEqualTo("peer-1");
        assertThat(peer.connected()).isTrue();
    }
}
