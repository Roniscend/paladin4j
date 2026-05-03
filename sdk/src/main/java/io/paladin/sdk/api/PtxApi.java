package io.paladin.sdk.api;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.paladin.sdk.core.JsonRpcTransport;
import io.paladin.sdk.model.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * API client for the {@code ptx_*} JSON-RPC namespace.
 *
 * <p>Provides methods for submitting, querying, and tracking transactions on the Paladin
 * privacy ledger. Each method is available in both synchronous and asynchronous variants.
 * Synchronous methods block the calling thread until the RPC response is received;
 * asynchronous methods return a {@link CompletableFuture} immediately.
 *
 * <p>This class is thread-safe and may be shared across multiple threads.
 *
 * @see TransactionInput
 * @see TransactionFull
 * @see TransactionReceipt
 */
public final class PtxApi {
    private final JsonRpcTransport transport;
    private final ObjectMapper mapper;

    /**
     * Creates a new {@code PtxApi} instance.
     *
     * @param transport the JSON-RPC transport used for sending requests
     * @param mapper    the Jackson {@link ObjectMapper} used for serialization
     */
    public PtxApi(JsonRpcTransport transport, ObjectMapper mapper) {
        this.transport = transport;
        this.mapper = mapper;
    }

    /**
     * Sends a transaction to the Paladin node.
     *
     * @param input the transaction input describing the operation
     * @return the transaction ID assigned by the node
     * @throws io.paladin.sdk.core.PaladinException if the RPC call fails or the node returns an error
     */
    public String sendTransaction(TransactionInput input) {
        return transport.invoke("ptx_sendTransaction", String.class, input);
    }

    /**
     * Asynchronously sends a transaction to the Paladin node.
     *
     * @param input the transaction input describing the operation
     * @return a future that completes with the transaction ID assigned by the node
     */
    public CompletableFuture<String> sendTransactionAsync(TransactionInput input) {
        return transport.invokeAsync("ptx_sendTransaction", String.class, input);
    }

    /**
     * Retrieves the full details of a transaction by its ID.
     *
     * @param txId the unique transaction identifier
     * @return the full transaction details, including receipt if available
     * @throws io.paladin.sdk.core.PaladinException if the RPC call fails or the transaction is not found
     */
    public TransactionFull getTransaction(String txId) {
        return transport.invoke("ptx_getTransactionFull", TransactionFull.class, txId);
    }

    /**
     * Asynchronously retrieves the full details of a transaction by its ID.
     *
     * @param txId the unique transaction identifier
     * @return a future that completes with the full transaction details
     */
    public CompletableFuture<TransactionFull> getTransactionAsync(String txId) {
        return transport.invokeAsync("ptx_getTransactionFull", TransactionFull.class, txId);
    }

    /**
     * Retrieves the receipt for a completed transaction.
     *
     * @param txId the unique transaction identifier
     * @return the transaction receipt, or {@code null} if the transaction has not yet completed
     * @throws io.paladin.sdk.core.PaladinException if the RPC call fails
     */
    public TransactionReceipt getTransactionReceipt(String txId) {
        return transport.invoke("ptx_getTransactionReceipt", TransactionReceipt.class, txId);
    }

    /**
     * Asynchronously retrieves the receipt for a completed transaction.
     *
     * @param txId the unique transaction identifier
     * @return a future that completes with the transaction receipt
     */
    public CompletableFuture<TransactionReceipt> getTransactionReceiptAsync(String txId) {
        return transport.invokeAsync("ptx_getTransactionReceipt", TransactionReceipt.class, txId);
    }

    /**
     * Prepares a transaction without submitting it, returning the prepared payload.
     *
     * <p>This is useful for inspecting the transaction before final submission,
     * or for obtaining metadata required by external signing flows.
     *
     * @param input the transaction input describing the operation
     * @return the prepared transaction containing the payload and metadata
     * @throws io.paladin.sdk.core.PaladinException if the RPC call fails
     */
    public PreparedTransaction prepareTransaction(TransactionInput input) {
        return transport.invoke("ptx_prepareTransaction", PreparedTransaction.class, input);
    }

    /**
     * Asynchronously prepares a transaction without submitting it.
     *
     * @param input the transaction input describing the operation
     * @return a future that completes with the prepared transaction
     */
    public CompletableFuture<PreparedTransaction> prepareTransactionAsync(TransactionInput input) {
        return transport.invokeAsync("ptx_prepareTransaction", PreparedTransaction.class, input);
    }

    /**
     * Queries transactions matching the given filter criteria.
     *
     * @param query the query filter built using {@link QueryJSON.Builder}
     * @return a list of matching transactions; may be empty but never {@code null}
     * @throws io.paladin.sdk.core.PaladinException if the RPC call fails
     */
    public List<TransactionFull> queryTransactions(QueryJSON query) {
        JavaType listType = mapper.getTypeFactory()
                .constructCollectionType(List.class, TransactionFull.class);
        return transport.invoke("ptx_queryTransactions", listType, query);
    }

    /**
     * Polls the node for a transaction receipt until it becomes available or the timeout expires.
     *
     * <p>This method uses an exponential polling strategy with a 500ms base interval.
     * If the receipt is not available within the specified timeout, the returned future
     * completes exceptionally with a {@link io.paladin.sdk.core.PaladinException}.
     *
     * @param txId    the unique transaction identifier to wait for
     * @param timeout the maximum duration to wait before giving up
     * @return a future that completes with the transaction receipt once confirmed
     */
    public CompletableFuture<TransactionReceipt> waitForReceipt(String txId, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        return pollReceipt(txId, deadline);
    }

    private CompletableFuture<TransactionReceipt> pollReceipt(String txId, long deadline) {
        if (System.currentTimeMillis() > deadline) {
            return CompletableFuture.failedFuture(
                    new io.paladin.sdk.core.PaladinException(
                            "Timeout waiting for receipt of transaction: " + txId));
        }
        return getTransactionReceiptAsync(txId)
                .thenCompose(receipt -> {
                    if (receipt != null) {
                        return CompletableFuture.completedFuture(receipt);
                    }
                    return CompletableFuture.runAsync(() -> {
                        try { Thread.sleep(500); } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).thenCompose(v -> pollReceipt(txId, deadline));
                })
                .exceptionallyCompose(ex -> {
                    if (System.currentTimeMillis() > deadline) {
                        return CompletableFuture.failedFuture(ex);
                    }
                    return CompletableFuture.runAsync(() -> {
                        try { Thread.sleep(500); } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).thenCompose(v -> pollReceipt(txId, deadline));
                });
    }
}
