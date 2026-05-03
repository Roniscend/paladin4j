package io.paladin.sdk.domain;

import io.paladin.sdk.api.KeymgrApi;
import io.paladin.sdk.api.PtxApi;
import io.paladin.sdk.model.*;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotoClientTest {

    private PtxApi ptx;
    private KeymgrApi keymgr;
    private NotoClient notoClient;

    @BeforeEach
    void setUp() {
        ptx = mock(PtxApi.class);
        keymgr = mock(KeymgrApi.class);
        notoClient = new NotoClient(ptx, keymgr);
    }

    @Test
    @DisplayName("deploy resolves key and returns contract address")
    void testDeploy() {
        when(keymgr.resolveKey("wallet", "notary@node1", "zarith"))
                .thenReturn(new KeyMappingAndVerifier("0xverifier", "wallet/notary", "zarith", "eth_address"));
        when(ptx.sendTransaction(any(TransactionInput.class)))
                .thenReturn("tx-deploy-001");
        when(ptx.waitForReceipt(eq("tx-deploy-001"), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new TransactionReceipt("tx-deploy-001", true, "0xtxhash", 1L,
                                "noto", null, "0xcontract", null, null)));

        String addr = notoClient.deploy("notary@node1", Map.of("name", "USD"));
        assertThat(addr).isEqualTo("0xcontract");
        verify(keymgr).resolveKey("wallet", "notary@node1", "zarith");
        verify(ptx).sendTransaction(any(TransactionInput.class));
    }

    @Test
    @DisplayName("mint sends async transaction and waits for receipt")
    void testMint() {
        when(ptx.sendTransactionAsync(any(TransactionInput.class)))
                .thenReturn(CompletableFuture.completedFuture("tx-mint-001"));
        when(ptx.waitForReceipt(eq("tx-mint-001"), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new TransactionReceipt("tx-mint-001", true, "0xhash", 2L,
                                "noto", null, null, null, null)));

        TransactionReceipt receipt = notoClient.mint("0xcontract", "notary@node1", "alice@node1", 5000).join();
        assertThat(receipt.success()).isTrue();
        verify(ptx).sendTransactionAsync(argThat(input ->
                input.function().equals("mint") && input.domain().equals("noto")));
    }

    @Test
    @DisplayName("transfer sends async transaction and waits for receipt")
    void testTransfer() {
        when(ptx.sendTransactionAsync(any(TransactionInput.class)))
                .thenReturn(CompletableFuture.completedFuture("tx-transfer-001"));
        when(ptx.waitForReceipt(eq("tx-transfer-001"), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new TransactionReceipt("tx-transfer-001", true, "0xhash", 3L,
                                "noto", null, null, null, null)));

        TransactionReceipt receipt = notoClient.transfer("0xcontract", "alice@node1", "bob@node2", 1000).join();
        assertThat(receipt.success()).isTrue();
        verify(ptx).sendTransactionAsync(argThat(input ->
                input.function().equals("transfer")));
    }

    @Test
    @DisplayName("approve sends async transaction and waits for receipt")
    void testApprove() {
        when(ptx.sendTransactionAsync(any(TransactionInput.class)))
                .thenReturn(CompletableFuture.completedFuture("tx-approve-001"));
        when(ptx.waitForReceipt(eq("tx-approve-001"), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new TransactionReceipt("tx-approve-001", true, "0xhash", 4L,
                                "noto", null, null, null, null)));

        TransactionReceipt receipt = notoClient.approve("0xcontract", "alice@node1", "delegate@node1", 500).join();
        assertThat(receipt.success()).isTrue();
        verify(ptx).sendTransactionAsync(argThat(input ->
                input.function().equals("approve")));
    }

    @Test
    @DisplayName("deploy throws PaladinException on failed receipt")
    void testDeployFailure() {
        when(keymgr.resolveKey("wallet", "notary@node1", "zarith"))
                .thenReturn(new KeyMappingAndVerifier("0xverifier", "wallet/notary", "zarith", "eth_address"));
        when(ptx.sendTransaction(any(TransactionInput.class)))
                .thenReturn("tx-fail-001");
        when(ptx.waitForReceipt(eq("tx-fail-001"), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new TransactionReceipt("tx-fail-001", false, null, 0L,
                                "noto", "gas limit exceeded", null, null, null)));

        assertThatThrownBy(() -> notoClient.deploy("notary@node1", Map.of("name", "BAD")))
                .isInstanceOf(io.paladin.sdk.core.PaladinException.class)
                .hasMessageContaining("Noto deploy failed");
    }
}
