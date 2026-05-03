package io.paladin.sdk.api;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.paladin.sdk.core.JsonRpcTransport;
import io.paladin.sdk.model.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * API client for the {@code bidx_*} JSON-RPC namespace (Block Indexer).
 *
 * <p>Provides methods for querying the indexed view of the underlying blockchain
 * including block headers, transaction metadata, and decoded event logs.
 *
 * @see IndexedBlock
 * @see IndexedTransaction
 * @see IndexedEvent
 */
public final class BidxApi {
    private final JsonRpcTransport transport;
    private final ObjectMapper mapper;

    /** Creates a new {@code BidxApi} instance. */
    public BidxApi(JsonRpcTransport transport, ObjectMapper mapper) {
        this.transport = transport;
        this.mapper = mapper;
    }

    /** Retrieves a block by its number. */
    public IndexedBlock getBlockByNumber(long blockNumber) {
        return transport.invoke("bidx_getBlockByNumber", IndexedBlock.class, blockNumber);
    }

    /** Retrieves a transaction by its on-chain hash. */
    public IndexedTransaction getTransactionByHash(String txHash) {
        return transport.invoke("bidx_getTransactionByHash", IndexedTransaction.class, txHash);
    }

    /** Queries indexed blocks matching the given filter criteria. */
    public List<IndexedBlock> queryIndexedBlocks(QueryJSON query) {
        JavaType listType = mapper.getTypeFactory()
                .constructCollectionType(List.class, IndexedBlock.class);
        return transport.invoke("bidx_queryIndexedBlocks", listType, query);
    }

    /** Queries indexed events matching the given filter criteria. */
    public List<IndexedEvent> queryIndexedEvents(QueryJSON query) {
        JavaType listType = mapper.getTypeFactory()
                .constructCollectionType(List.class, IndexedEvent.class);
        return transport.invoke("bidx_queryIndexedEvents", listType, query);
    }

    /** Queries indexed transactions matching the given filter criteria. */
    public List<IndexedTransaction> queryIndexedTransactions(QueryJSON query) {
        JavaType listType = mapper.getTypeFactory()
                .constructCollectionType(List.class, IndexedTransaction.class);
        return transport.invoke("bidx_queryIndexedTransactions", listType, query);
    }

    /** Asynchronously retrieves a block by its number. */
    public CompletableFuture<IndexedBlock> getBlockByNumberAsync(long blockNumber) {
        return transport.invokeAsync("bidx_getBlockByNumber", IndexedBlock.class, blockNumber);
    }
}
