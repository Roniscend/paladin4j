package io.paladin.sdk.api;
import io.paladin.sdk.core.JsonRpcTransport;
import io.paladin.sdk.model.*;
import java.util.concurrent.CompletableFuture;

/**
 * API client for the {@code transport_*} JSON-RPC namespace.
 *
 * <p>Provides methods for querying peer-to-peer transport information,
 * including the local node's identity and remote peer connection status.
 *
 * @see PeerInfo
 */
public final class TransportApi {
    private final JsonRpcTransport transport;

    /** Creates a new {@code TransportApi} instance. */
    public TransportApi(JsonRpcTransport transport) {
        this.transport = transport;
    }

    /**
     * Retrieves the local node's peer information.
     *
     * @return peer info including ID, name, and endpoint
     */
    public PeerInfo getLocalPeerInfo() {
        return transport.invoke("transport_getLocalPeerInfo", PeerInfo.class);
    }

    /**
     * Retrieves information about a remote peer by its ID.
     *
     * @param peerId the unique peer identifier
     * @return the peer's connection info
     */
    public PeerInfo getPeerInfo(String peerId) {
        return transport.invoke("transport_getPeerInfo", PeerInfo.class, peerId);
    }

    /** Asynchronously retrieves the local node's peer information. */
    public CompletableFuture<PeerInfo> getLocalPeerInfoAsync() {
        return transport.invokeAsync("transport_getLocalPeerInfo", PeerInfo.class);
    }
}
