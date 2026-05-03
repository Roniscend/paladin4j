package io.paladin.sdk.api;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.paladin.sdk.core.JsonRpcTransport;
import io.paladin.sdk.model.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * API client for the {@code pgroup_*} JSON-RPC namespace.
 *
 * <p>Provides methods for creating and managing privacy groups. Privacy groups
 * are the fundamental unit of private collaboration in Paladin, allowing a set
 * of parties to share encrypted state and execute private transactions.
 *
 * <p>This class is thread-safe and may be shared across multiple threads.
 *
 * @see PrivacyGroup
 * @see PrivacyGroupInput
 */
public final class PgroupApi {
    private final JsonRpcTransport transport;
    private final ObjectMapper mapper;

    /**
     * Creates a new {@code PgroupApi} instance.
     *
     * @param transport the JSON-RPC transport used for sending requests
     * @param mapper    the Jackson {@link ObjectMapper} used for serialization
     */
    public PgroupApi(JsonRpcTransport transport, ObjectMapper mapper) {
        this.transport = transport;
        this.mapper = mapper;
    }

    /**
     * Creates a new privacy group on the Paladin node.
     *
     * @param input the privacy group specification including domain, name, and members
     * @return the created privacy group with its assigned ID and contract address
     * @throws io.paladin.sdk.core.PaladinException if the RPC call fails
     */
    public PrivacyGroup createGroup(PrivacyGroupInput input) {
        return transport.invoke("pgroup_create", PrivacyGroup.class, input);
    }

    /**
     * Asynchronously creates a new privacy group on the Paladin node.
     *
     * @param input the privacy group specification
     * @return a future that completes with the created privacy group
     */
    public CompletableFuture<PrivacyGroup> createGroupAsync(PrivacyGroupInput input) {
        return transport.invokeAsync("pgroup_create", PrivacyGroup.class, input);
    }

    /**
     * Executes a read-only call against a privacy group's contract.
     *
     * <p>This is similar to an Ethereum {@code eth_call} but operates within
     * the privacy group's private EVM execution context.
     *
     * @param groupId  the unique identifier of the privacy group
     * @param callData the call parameters including function selector and arguments
     * @return the raw JSON result of the call
     * @throws io.paladin.sdk.core.PaladinException if the RPC call fails
     */
    public JsonNode call(String groupId, Map<String, Object> callData) {
        return transport.invoke("pgroup_call", JsonNode.class, groupId, callData);
    }

    /**
     * Sends an encrypted message to the members of a privacy group.
     *
     * @param groupId     the unique identifier of the privacy group
     * @param messageData the message payload to send
     * @return the message ID assigned by the node
     * @throws io.paladin.sdk.core.PaladinException if the RPC call fails
     */
    public String sendMessage(String groupId, Map<String, Object> messageData) {
        return transport.invoke("pgroup_sendMessage", String.class, groupId, messageData);
    }

    /**
     * Retrieves the details of a privacy group by its ID.
     *
     * @param groupId the unique identifier of the privacy group
     * @return the privacy group details including members and contract address
     * @throws io.paladin.sdk.core.PaladinException if the RPC call fails or the group is not found
     */
    public PrivacyGroup getGroup(String groupId) {
        return transport.invoke("pgroup_getGroup", PrivacyGroup.class, groupId);
    }
}
