package io.paladin.sdk.domain;

import io.paladin.sdk.api.KeymgrApi;
import io.paladin.sdk.api.PtxApi;
import io.paladin.sdk.model.*;
import org.junit.jupiter.api.*;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ZetoClientTest {

    private PtxApi ptx;
    private KeymgrApi keymgr;
    private ZetoClient zetoClient;

    @BeforeEach
    void setUp() {
        ptx = mock(PtxApi.class);
        keymgr = mock(KeymgrApi.class);
        zetoClient = new ZetoClient(ptx, keymgr);
    }

    @Test
    @DisplayName("deploy sends constructor transaction and returns txId")
    void testDeploy() {
        when(ptx.sendTransaction(any(TransactionInput.class)))
                .thenReturn("tx-zeto-deploy");
        String txId = zetoClient.deploy("deployer@node1", "ZKToken", true);
        assertThat(txId).isEqualTo("tx-zeto-deploy");
        verify(ptx).sendTransaction(argThat(input ->
                input.domain().equals("zeto") && input.function().equals("constructor")));
    }

    @Test
    @DisplayName("mint sends async transaction and waits for receipt")
    void testMint() {
        when(ptx.sendTransactionAsync(any(TransactionInput.class)))
                .thenReturn(CompletableFuture.completedFuture("tx-zeto-mint"));
        when(ptx.waitForReceipt(eq("tx-zeto-mint"), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new TransactionReceipt("tx-zeto-mint", true, "0xhash", 5L,
                                "zeto", null, null, null, null)));

        TransactionReceipt receipt = zetoClient.mint("0xcontract", "minter@node1", "alice@node1", 1000).join();
        assertThat(receipt.success()).isTrue();
        verify(ptx).sendTransactionAsync(argThat(input ->
                input.function().equals("mint") && input.domain().equals("zeto")));
    }

    @Test
    @DisplayName("transfer sends async transaction and waits for receipt")
    void testTransfer() {
        when(ptx.sendTransactionAsync(any(TransactionInput.class)))
                .thenReturn(CompletableFuture.completedFuture("tx-zeto-transfer"));
        when(ptx.waitForReceipt(eq("tx-zeto-transfer"), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new TransactionReceipt("tx-zeto-transfer", true, "0xhash", 6L,
                                "zeto", null, null, null, null)));

        TransactionReceipt receipt = zetoClient.transfer("0xcontract", "alice@node1", "bob@node2", 500).join();
        assertThat(receipt.success()).isTrue();
    }
}
