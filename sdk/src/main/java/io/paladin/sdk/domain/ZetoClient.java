package io.paladin.sdk.domain;
import io.paladin.sdk.api.PtxApi;
import io.paladin.sdk.api.KeymgrApi;
import io.paladin.sdk.model.*;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * High-level client for the <strong>Zeto</strong> privacy domain.
 *
 * <p>Zeto (Zero-Knowledge Tokens for Ethereum) provides privacy-preserving token
 * operations using zero-knowledge proofs. This client simplifies deploying Zeto
 * contracts and performing shielded mint/transfer operations.
 *
 * @see PtxApi
 * @see KeymgrApi
 */
public final class ZetoClient {
    private final PtxApi ptx;
    private final KeymgrApi keymgr;
    private static final String DOMAIN = "zeto";
    private static final Duration DEFAULT_RECEIPT_TIMEOUT = Duration.ofSeconds(60);

    /** Creates a new {@code ZetoClient} backed by the given API clients. */
    public ZetoClient(PtxApi ptx, KeymgrApi keymgr) {
        this.ptx = ptx;
        this.keymgr = keymgr;
    }

    /**
     * Deploys a new Zeto token contract.
     *
     * @param from          the deployer identity
     * @param tokenName     the name of the token
     * @param enableHistory whether to enable transaction history tracking
     * @return the transaction ID of the deployment
     */
    public String deploy(String from, String tokenName, boolean enableHistory) {
        return ptx.sendTransaction(TransactionInput.builder()
                .type(TransactionType.PRIVATE)
                .domain(DOMAIN)
                .from(from)
                .function("constructor")
                .data(Map.of("tokenName", tokenName, "enableHistory", enableHistory))
                .build());
    }

    /**
     * Mints new Zeto tokens to a recipient.
     *
     * @param contractAddress the Zeto contract address
     * @param from            the minting authority identity
     * @param to              the recipient identity
     * @param amount          the amount of tokens to mint
     * @return a future that completes with the transaction receipt
     */
    public CompletableFuture<TransactionReceipt> mint(String contractAddress, String from,
                                                       String to, long amount) {
        return ptx.sendTransactionAsync(TransactionInput.builder()
                .type(TransactionType.PRIVATE)
                .domain(DOMAIN)
                .from(from)
                .to(contractAddress)
                .function("mint")
                .data(Map.of("to", to, "amount", amount))
                .build())
                .thenCompose(txId -> ptx.waitForReceipt(txId, DEFAULT_RECEIPT_TIMEOUT));
    }

    /**
     * Transfers Zeto tokens from the sender to a recipient.
     *
     * @param contractAddress the Zeto contract address
     * @param from            the sender identity
     * @param to              the recipient identity
     * @param amount          the amount of tokens to transfer
     * @return a future that completes with the transaction receipt
     */
    public CompletableFuture<TransactionReceipt> transfer(String contractAddress, String from,
                                                           String to, long amount) {
        return ptx.sendTransactionAsync(TransactionInput.builder()
                .type(TransactionType.PRIVATE)
                .domain(DOMAIN)
                .from(from)
                .to(contractAddress)
                .function("transfer")
                .data(Map.of("to", to, "amount", amount))
                .build())
                .thenCompose(txId -> ptx.waitForReceipt(txId, DEFAULT_RECEIPT_TIMEOUT));
    }
}
