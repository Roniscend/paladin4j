package io.paladin.sdk.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.paladin.sdk.api.PgroupApi;
import io.paladin.sdk.api.PtxApi;
import io.paladin.sdk.model.*;
import org.junit.jupiter.api.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PenteClientTest {

    private PtxApi ptx;
    private PgroupApi pgroup;
    private PenteClient penteClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ptx = mock(PtxApi.class);
        pgroup = mock(PgroupApi.class);
        penteClient = new PenteClient(ptx, pgroup);
    }

    @Test
    @DisplayName("createGroup delegates to PgroupApi with pente domain")
    void testCreateGroup() {
        when(pgroup.createGroup(any(PrivacyGroupInput.class)))
                .thenReturn(new PrivacyGroup("group-001", "pente", "myGroup",
                        new String[]{"alice@node1", "bob@node2"}, null, "0xcontract", null));

        PrivacyGroup group = penteClient.createGroup("myGroup", "alice@node1", "bob@node2");
        assertThat(group.id()).isEqualTo("group-001");
        assertThat(group.domain()).isEqualTo("pente");
        verify(pgroup).createGroup(argThat(input ->
                input.domain().equals("pente") && input.name().equals("myGroup")));
    }

    @Test
    @DisplayName("deployContract sends private transaction and waits for receipt")
    void testDeployContract() {
        when(ptx.sendTransactionAsync(any(TransactionInput.class)))
                .thenReturn(CompletableFuture.completedFuture("tx-deploy"));
        when(ptx.waitForReceipt(eq("tx-deploy"), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new TransactionReceipt("tx-deploy", true, "0xhash", 10L,
                                "pente", null, "0xnewcontract", null, null)));

        JsonNode abi = mapper.createArrayNode();
        TransactionReceipt receipt = penteClient.deployContract(
                "0xgroup", "deployer@node1", abi, "0x600160...", Map.of("param1", "value1")).join();
        assertThat(receipt.success()).isTrue();
        assertThat(receipt.contractAddress()).isEqualTo("0xnewcontract");
    }

    @Test
    @DisplayName("invoke sends function call and waits for receipt")
    void testInvoke() {
        when(ptx.sendTransactionAsync(any(TransactionInput.class)))
                .thenReturn(CompletableFuture.completedFuture("tx-invoke"));
        when(ptx.waitForReceipt(eq("tx-invoke"), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new TransactionReceipt("tx-invoke", true, "0xhash", 11L,
                                "pente", null, null, null, null)));

        TransactionReceipt receipt = penteClient.invoke(
                "0xgroup", "caller@node1", "setValue", Map.of("key", "value")).join();
        assertThat(receipt.success()).isTrue();
        verify(ptx).sendTransactionAsync(argThat(input ->
                input.function().equals("setValue") && input.domain().equals("pente")));
    }

    @Test
    @DisplayName("call delegates to PgroupApi")
    void testCall() {
        JsonNode mockResult = mapper.createObjectNode().put("result", 42);
        when(pgroup.call(eq("group-001"), anyMap()))
                .thenReturn(mockResult);

        JsonNode result = penteClient.call("group-001", Map.of("method", "getValue"));
        assertThat(result.get("result").asInt()).isEqualTo(42);
        verify(pgroup).call("group-001", Map.of("method", "getValue"));
    }
}
