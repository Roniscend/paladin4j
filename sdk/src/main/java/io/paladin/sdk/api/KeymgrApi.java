package io.paladin.sdk.api;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.paladin.sdk.core.JsonRpcTransport;
import io.paladin.sdk.model.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * API client for the {@code keymgr_*} JSON-RPC namespace.
 *
 * <p>Provides methods for resolving cryptographic key identities, performing
 * reverse lookups from on-chain verifiers to wallet paths, and querying the
 * node's key inventory. Paladin uses hierarchical key paths (e.g., "wallet/owner1")
 * to organize keys across multiple algorithms.
 *
 * <p>This class is thread-safe and may be shared across multiple threads.
 *
 * @see KeyMappingAndVerifier
 */
public final class KeymgrApi {
    private final JsonRpcTransport transport;
    private final ObjectMapper mapper;

    /**
     * Creates a new {@code KeymgrApi} instance.
     *
     * @param transport the JSON-RPC transport used for sending requests
     * @param mapper    the Jackson {@link ObjectMapper} used for serialization
     */
    public KeymgrApi(JsonRpcTransport transport, ObjectMapper mapper) {
        this.transport = transport;
        this.mapper = mapper;
    }

    /**
     * Resolves a wallet key path to its on-chain verifier and metadata.
     *
     * <p>If the key does not yet exist in the node's key store, it will be
     * generated on demand using the specified algorithm.
     *
     * @param wallet    the wallet name (e.g., "wallet")
     * @param keyPath   the hierarchical key path (e.g., "owner1@node1")
     * @param algorithm the signing algorithm (e.g., "zarith", "secp256k1")
     * @return the key mapping containing the on-chain verifier address
     * @throws io.paladin.sdk.core.PaladinException if the RPC call fails
     */
    public KeyMappingAndVerifier resolveKey(String wallet, String keyPath, String algorithm) {
        return transport.invoke("keymgr_resolveKey", KeyMappingAndVerifier.class,
                wallet, keyPath, algorithm);
    }

    /**
     * Asynchronously resolves a wallet key path to its on-chain verifier.
     *
     * @param wallet    the wallet name
     * @param keyPath   the hierarchical key path
     * @param algorithm the signing algorithm
     * @return a future that completes with the key mapping
     */
    public CompletableFuture<KeyMappingAndVerifier> resolveKeyAsync(
            String wallet, String keyPath, String algorithm) {
        return transport.invokeAsync("keymgr_resolveKey", KeyMappingAndVerifier.class,
                wallet, keyPath, algorithm);
    }

    /**
     * Performs a reverse lookup from an on-chain verifier to the local key mapping.
     *
     * @param algorithm the signing algorithm used to create the verifier
     * @param verifier  the on-chain verifier address (e.g., Ethereum address)
     * @return the key mapping if the verifier is known locally
     * @throws io.paladin.sdk.core.PaladinException if the verifier is not found or the RPC call fails
     */
    public KeyMappingAndVerifier reverseKeyLookup(String algorithm, String verifier) {
        return transport.invoke("keymgr_reverseKeyLookup", KeyMappingAndVerifier.class,
                algorithm, verifier);
    }

    /**
     * Queries keys matching the given filter criteria.
     *
     * @param query the query filter built using {@link QueryJSON.Builder}
     * @return a list of matching key mappings; may be empty but never {@code null}
     * @throws io.paladin.sdk.core.PaladinException if the RPC call fails
     */
    public List<KeyMappingAndVerifier> queryKeys(QueryJSON query) {
        JavaType listType = mapper.getTypeFactory()
                .constructCollectionType(List.class, KeyMappingAndVerifier.class);
        return transport.invoke("keymgr_queryKeys", listType, query);
    }
}
