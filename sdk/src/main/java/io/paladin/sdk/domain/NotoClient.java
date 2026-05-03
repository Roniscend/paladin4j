package io.paladin.sdk.domain;
import io.paladin.sdk.api.PtxApi;
import io.paladin.sdk.api.KeymgrApi;
import io.paladin.sdk.model.*;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * High-level client for the <strong>Noto</strong> privacy domain.
 *
 * <p>Noto is Paladin's UTXO-based privacy token framework. This client provides
 * a simplified interface for deploying Noto token contracts, minting, transferring,
 * and approving token amounts — all within the privacy domain's shielded execution context.
 *
 * <p>Under the hood, each operation constructs a {@link TransactionInput}, submits it
 * via the {@link PtxApi}, and optionally waits for a confirmed {@link TransactionReceipt}.
 *
 * <p><strong>Example usage:</strong></p>
 * <pre>{@code
 * try (var client = PaladinClient.builder().endpoint("http://localhost:31548").build()) {
 *     String addr = client.noto().deploy("notary@node1", Map.of("name", "USDToken"));
 *     client.noto().mint(addr, "notary@node1", "recipient@node1", 5000).join();
 *     client.noto().transfer(addr, "recipient@node1", "other@node2", 1000).join();
 * }
 * }</pre>
 *
 * @see PtxApi
 * @see KeymgrApi
 */
public final class NotoClient {
    private final PtxApi ptx;
    private final KeymgrApi keymgr;
    private static final String DOMAIN = "noto";
    private static final Duration DEFAULT_RECEIPT_TIMEOUT = Duration.ofSeconds(30);

    /** Creates a new {@code NotoClient} backed by the given API clients. */
    public NotoClient(PtxApi ptx, KeymgrApi keymgr) {
        this.ptx = ptx;
        this.keymgr = keymgr;
    }

    /**
     * Deploys a new Noto token contract.
     *
     * <p>This is a blocking operation that resolves the notary key, submits the
     * deployment transaction, and waits for the receipt to obtain the contract address.
     *
     * @param notary     the notary identity (e.g., "notary@node1")
     * @param properties additional contract properties (e.g., token name)
     * @return the deployed contract address
     * @throws io.paladin.sdk.core.PaladinException if deployment fails or times out
     */
    public String deploy(String notary, Map<String, Object> properties) {
        KeyMappingAndVerifier key = keymgr.resolveKey("wallet", notary, "zarith");
        var mutableProps = new java.util.HashMap<>(properties);
        mutableProps.put("notary", key.verifier());
        String txId = ptx.sendTransaction(TransactionInput.builder()
                .type(TransactionType.PRIVATE)
                .domain(DOMAIN)
                .from(notary)
                .data(mutableProps)
                .function("constructor")
                .build());
        TransactionReceipt receipt = ptx.waitForReceipt(txId, DEFAULT_RECEIPT_TIMEOUT).join();
        if (receipt.isFailed()) {
            throw new io.paladin.sdk.core.PaladinException(
                    "Noto deploy failed: " + receipt.failureMessage());
        }
        return receipt.contractAddress();
    }

    /**
     * Mints new tokens to a recipient.
     *
     * @param contractAddress the Noto contract address
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
     * Transfers tokens from the sender to a recipient.
     *
     * @param contractAddress the Noto contract address
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

    /**
     * Approves a delegate to spend tokens on behalf of the owner.
     *
     * @param contractAddress the Noto contract address
     * @param from            the token owner identity
     * @param delegate        the delegate identity being approved
     * @param amount          the approved spending amount
     * @return a future that completes with the transaction receipt
     */
    public CompletableFuture<TransactionReceipt> approve(String contractAddress, String from,
                                                          String delegate, long amount) {
        return ptx.sendTransactionAsync(TransactionInput.builder()
                .type(TransactionType.PRIVATE)
                .domain(DOMAIN)
                .from(from)
                .to(contractAddress)
                .function("approve")
                .data(Map.of("delegate", delegate, "amount", amount))
                .build())
                .thenCompose(txId -> ptx.waitForReceipt(txId, DEFAULT_RECEIPT_TIMEOUT));
    }
}
