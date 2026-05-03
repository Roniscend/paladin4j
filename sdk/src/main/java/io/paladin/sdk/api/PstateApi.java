package io.paladin.sdk.api;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.paladin.sdk.core.JsonRpcTransport;
import io.paladin.sdk.model.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * API client for the {@code pstate_*} JSON-RPC namespace.
 *
 * <p>Provides methods for querying privacy-domain contract states and schemas.
 * States represent the current or historical data held within a privacy domain
 * (e.g., Noto UTXO states, Zeto commitments).
 *
 * <p>This class is thread-safe and may be shared across multiple threads.
 *
 * @see State
 * @see Schema
 * @see QueryJSON
 */
public final class PstateApi {
    private final JsonRpcTransport transport;
    private final ObjectMapper mapper;

    /**
     * Creates a new {@code PstateApi} instance.
     *
     * @param transport the JSON-RPC transport used for sending requests
     * @param mapper    the Jackson {@link ObjectMapper} used for serialization
     */
    public PstateApi(JsonRpcTransport transport, ObjectMapper mapper) {
        this.transport = transport;
        this.mapper = mapper;
    }

    /**
     * Queries contract states within a specific privacy domain and contract.
     *
     * @param domainName      the name of the privacy domain (e.g., "noto", "zeto")
     * @param contractAddress the on-chain address of the privacy contract
     * @param schemaId        the schema identifier for the state type
     * @param query           the query filter built using {@link QueryJSON.Builder}
     * @param statusQualifier state status filter (e.g., "available", "spent", "all")
     * @return a list of matching states; may be empty but never {@code null}
     * @throws io.paladin.sdk.core.PaladinException if the RPC call fails
     */
    public List<State> queryContractStates(String domainName, String contractAddress,
                                           String schemaId, QueryJSON query, String statusQualifier) {
        JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, State.class);
        return transport.invoke("pstate_queryContractStates", listType,
                domainName, contractAddress, schemaId, query, statusQualifier);
    }

    /**
     * Asynchronously queries contract states within a specific privacy domain and contract.
     *
     * @param domainName      the name of the privacy domain
     * @param contractAddress the on-chain address of the privacy contract
     * @param schemaId        the schema identifier for the state type
     * @param query           the query filter
     * @param statusQualifier state status filter
     * @return a future that completes with a list of matching states
     */
    public CompletableFuture<List<State>> queryContractStatesAsync(
            String domainName, String contractAddress,
            String schemaId, QueryJSON query, String statusQualifier) {
        JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, State.class);
        return transport.invokeAsync("pstate_queryContractStates", listType,
                domainName, contractAddress, schemaId, query, statusQualifier);
    }

    /**
     * Retrieves a schema definition by its domain and schema ID.
     *
     * @param domainName the name of the privacy domain
     * @param schemaId   the unique schema identifier
     * @return the schema definition
     * @throws io.paladin.sdk.core.PaladinException if the RPC call fails or the schema is not found
     */
    public Schema getSchema(String domainName, String schemaId) {
        return transport.invoke("pstate_getSchema", Schema.class, domainName, schemaId);
    }

    /**
     * Lists all schemas registered for a specific contract within a privacy domain.
     *
     * @param domainName      the name of the privacy domain
     * @param contractAddress the on-chain address of the privacy contract
     * @return a list of schemas; may be empty but never {@code null}
     * @throws io.paladin.sdk.core.PaladinException if the RPC call fails
     */
    public List<Schema> listSchemas(String domainName, String contractAddress) {
        JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, Schema.class);
        return transport.invoke("pstate_listContractSchemas", listType, domainName, contractAddress);
    }
}
