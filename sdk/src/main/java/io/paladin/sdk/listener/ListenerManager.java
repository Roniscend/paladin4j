package io.paladin.sdk.listener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.paladin.sdk.core.JsonRpcRequest;
import io.paladin.sdk.core.WebSocketTransport;
import io.paladin.sdk.model.TransactionReceipt;
import io.paladin.sdk.model.IndexedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages WebSocket event subscriptions for real-time event streaming from a Paladin node.
 *
 * <p>The {@code ListenerManager} allows registering typed callbacks that are invoked
 * automatically when the node pushes events over the WebSocket connection. Supported
 * event types include transaction receipts, blockchain events, and privacy group messages.
 *
 * <p>This class is thread-safe. Listeners can be added and removed concurrently.
 * Implements {@link AutoCloseable} to clean up all active subscriptions.
 *
 * <p><strong>Example:</strong></p>
 * <pre>{@code
 * client.listen().onReceipt("myListener", receipt -> {
 *     System.out.println("Tx confirmed: " + receipt.id());
 * });
 * }</pre>
 */
public final class ListenerManager implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ListenerManager.class);
    private final WebSocketTransport wsTransport;
    private final ObjectMapper mapper;
    private final Map<String, ListenerRegistration> activeListeners = new ConcurrentHashMap<>();

    /** Creates a new {@code ListenerManager} using the given WebSocket transport. */
    public ListenerManager(WebSocketTransport wsTransport, ObjectMapper mapper) {
        this.wsTransport = wsTransport;
        this.mapper = mapper;
    }

    /**
     * Registers a listener for transaction receipt events.
     *
     * @param listenerId a unique identifier for this listener
     * @param handler    callback invoked with the deserialized receipt on each event
     */
    public void onReceipt(String listenerId, Consumer<TransactionReceipt> handler) {
        wsTransport.subscribe(listenerId, node -> {
            try {
                TransactionReceipt receipt = mapper.treeToValue(node, TransactionReceipt.class);
                handler.accept(receipt);
            } catch (Exception e) {
                log.error("Failed to deserialize receipt event for listener {}", listenerId, e);
            }
        });
        wsTransport.send(JsonRpcRequest.create("ptx_subscribe", listenerId, Map.of(
                "type", "receipt"
        )));
        activeListeners.put(listenerId, new ListenerRegistration(listenerId, "receipt"));
        log.info("Registered receipt listener: {}", listenerId);
    }

    /**
     * Registers a listener for blockchain events (decoded log entries).
     *
     * @param listenerId a unique identifier for this listener
     * @param handler    callback invoked with the deserialized event on each event
     */
    public void onBlockchainEvent(String listenerId, Consumer<IndexedEvent> handler) {
        wsTransport.subscribe(listenerId, node -> {
            try {
                IndexedEvent event = mapper.treeToValue(node, IndexedEvent.class);
                handler.accept(event);
            } catch (Exception e) {
                log.error("Failed to deserialize blockchain event for listener {}", listenerId, e);
            }
        });
        wsTransport.send(JsonRpcRequest.create("bidx_subscribe", listenerId, Map.of(
                "type", "event"
        )));
        activeListeners.put(listenerId, new ListenerRegistration(listenerId, "event"));
        log.info("Registered blockchain event listener: {}", listenerId);
    }

    /**
     * Registers a listener for messages within a privacy group.
     *
     * @param listenerId a unique identifier for this listener
     * @param groupId    the privacy group ID to listen on
     * @param handler    callback invoked with the raw JSON message payload
     */
    public void onGroupMessage(String listenerId, String groupId, Consumer<JsonNode> handler) {
        wsTransport.subscribe(listenerId, handler::accept);
        wsTransport.send(JsonRpcRequest.create("pgroup_subscribe", listenerId, Map.of(
                "type", "message",
                "groupId", groupId
        )));
        activeListeners.put(listenerId, new ListenerRegistration(listenerId, "message"));
        log.info("Registered group message listener: {} for group {}", listenerId, groupId);
    }

    /**
     * Removes and unsubscribes a previously registered listener.
     *
     * @param listenerId the unique identifier of the listener to remove
     */
    public void removeListener(String listenerId) {
        wsTransport.unsubscribe(listenerId);
        activeListeners.remove(listenerId);
        log.info("Removed listener: {}", listenerId);
    }

    /** Returns the number of currently active listeners. */
    public int getActiveListenerCount() {
        return activeListeners.size();
    }

    @Override
    public void close() {
        activeListeners.keySet().forEach(this::removeListener);
    }

    private record ListenerRegistration(String id, String type) {}
}
