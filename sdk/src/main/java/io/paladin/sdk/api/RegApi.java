package io.paladin.sdk.api;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.paladin.sdk.core.JsonRpcTransport;
import io.paladin.sdk.model.*;
import java.util.List;

/**
 * API client for the {@code reg_*} JSON-RPC namespace.
 *
 * <p>Provides methods for querying the Paladin on-chain registry, which stores
 * metadata entries such as domain registrations and contract deployments.
 *
 * @see RegistryEntry
 */
public final class RegApi {
    private final JsonRpcTransport transport;
    private final ObjectMapper mapper;

    /** Creates a new {@code RegApi} instance. */
    public RegApi(JsonRpcTransport transport, ObjectMapper mapper) {
        this.transport = transport;
        this.mapper = mapper;
    }

    /**
     * Queries registry entries matching the given filter criteria.
     *
     * @param registry the registry name to query
     * @param query    the query filter
     * @return a list of matching registry entries
     */
    public List<RegistryEntry> getRegistryEntries(String registry, QueryJSON query) {
        JavaType listType = mapper.getTypeFactory()
                .constructCollectionType(List.class, RegistryEntry.class);
        return transport.invoke("reg_getRegistryEntries", listType, registry, query);
    }

    /**
     * Retrieves a specific registry entry by its unique ID.
     *
     * @param registry the registry name
     * @param entryId  the unique entry identifier
     * @return the registry entry
     */
    public RegistryEntry getRegistryEntryByID(String registry, String entryId) {
        return transport.invoke("reg_getRegistryEntryByID", RegistryEntry.class, registry, entryId);
    }
}
