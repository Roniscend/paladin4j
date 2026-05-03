package io.paladin.sdk;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.paladin.sdk.api.*;
import io.paladin.sdk.core.*;
import io.paladin.sdk.crypto.LocalSigner;
import io.paladin.sdk.domain.*;
import io.paladin.sdk.listener.ListenerManager;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
/**
 * Main entry point for the Paladin Java SDK.
 *
 * <p>Provides access to all Paladin JSON-RPC API namespaces (ptx, pstate, pgroup,
 * keymgr, bidx, reg, transport) as well as high-level domain helpers for Noto,
 * Zeto, and Pente privacy domains. Also supports real-time event streaming via WebSocket.
 *
 * <p>Instances are created using the {@link Builder} pattern and implement
 * {@link AutoCloseable} for proper resource cleanup:
 *
 * <pre>{@code
 * try (var client = PaladinClient.builder()
 *         .endpoint("http://127.0.0.1:31548")
 *         .maxRetries(3)
 *         .build()) {
 *     String addr = client.noto().deploy("notary@node1", Map.of("name", "USD"));
 *     client.noto().mint(addr, "notary@node1", "alice@node1", 5000).join();
 * }
 * }</pre>
 *
 * <p>All API accessors use thread-safe lazy initialization (double-checked locking)
 * and return singleton instances, so this client can be safely shared across threads.
 *
 * @see PaladinClient.Builder
 */
public final class PaladinClient implements AutoCloseable {
    private final TransportConfig config;
    private final ObjectMapper objectMapper;
    private final JsonRpcTransport rpcTransport;
    private final WebSocketTransport wsTransport;
    private final LocalSigner signer;
    private volatile PtxApi ptxApi;
    private volatile PstateApi pstateApi;
    private volatile PgroupApi pgroupApi;
    private volatile KeymgrApi keymgrApi;
    private volatile BidxApi bidxApi;
    private volatile RegApi regApi;
    private volatile TransportApi transportApi;
    private volatile NotoClient notoClient;
    private volatile ZetoClient zetoClient;
    private volatile PenteClient penteClient;
    private volatile ListenerManager listenerManager;
    private PaladinClient(TransportConfig config, LocalSigner signer) {
        this.config = config;
        this.signer = signer;
        this.objectMapper = createObjectMapper();
        this.rpcTransport = new JsonRpcTransport(config, objectMapper);
        this.wsTransport = new WebSocketTransport(config, objectMapper);
    }
    public static Builder builder() {
        return new Builder();
    }
    public PtxApi ptx() {
        if (ptxApi == null) {
            synchronized (this) {
                if (ptxApi == null) ptxApi = new PtxApi(rpcTransport, objectMapper);
            }
        }
        return ptxApi;
    }
    public PstateApi pstate() {
        if (pstateApi == null) {
            synchronized (this) {
                if (pstateApi == null) pstateApi = new PstateApi(rpcTransport, objectMapper);
            }
        }
        return pstateApi;
    }
    public PgroupApi pgroup() {
        if (pgroupApi == null) {
            synchronized (this) {
                if (pgroupApi == null) pgroupApi = new PgroupApi(rpcTransport, objectMapper);
            }
        }
        return pgroupApi;
    }
    public KeymgrApi keymgr() {
        if (keymgrApi == null) {
            synchronized (this) {
                if (keymgrApi == null) keymgrApi = new KeymgrApi(rpcTransport, objectMapper);
            }
        }
        return keymgrApi;
    }
    public BidxApi bidx() {
        if (bidxApi == null) {
            synchronized (this) {
                if (bidxApi == null) bidxApi = new BidxApi(rpcTransport, objectMapper);
            }
        }
        return bidxApi;
    }
    public RegApi reg() {
        if (regApi == null) {
            synchronized (this) {
                if (regApi == null) regApi = new RegApi(rpcTransport, objectMapper);
            }
        }
        return regApi;
    }
    public TransportApi transport() {
        if (transportApi == null) {
            synchronized (this) {
                if (transportApi == null) transportApi = new TransportApi(rpcTransport);
            }
        }
        return transportApi;
    }
    public NotoClient noto() {
        if (notoClient == null) {
            synchronized (this) {
                if (notoClient == null) notoClient = new NotoClient(ptx(), keymgr());
            }
        }
        return notoClient;
    }
    public ZetoClient zeto() {
        if (zetoClient == null) {
            synchronized (this) {
                if (zetoClient == null) zetoClient = new ZetoClient(ptx(), keymgr());
            }
        }
        return zetoClient;
    }
    public PenteClient pente() {
        if (penteClient == null) {
            synchronized (this) {
                if (penteClient == null) penteClient = new PenteClient(ptx(), pgroup());
            }
        }
        return penteClient;
    }
    public ListenerManager listen() {
        if (listenerManager == null) {
            synchronized (this) {
                if (listenerManager == null) {
                    wsTransport.connect().join();
                    listenerManager = new ListenerManager(wsTransport, objectMapper);
                }
            }
        }
        return listenerManager;
    }
    public CompletableFuture<ListenerManager> listenAsync() {
        if (listenerManager == null) {
            synchronized (this) {
                if (listenerManager == null) {
                    ListenerManager mgr = new ListenerManager(wsTransport, objectMapper);
                    return wsTransport.connect().thenApply(v -> {
                        this.listenerManager = mgr;
                        return mgr;
                    });
                }
            }
        }
        return CompletableFuture.completedFuture(listenerManager);
    }
    /**
     * Returns the local signer configured for this client, or {@code null} if none was set.
     *
     * @return the {@link LocalSigner}, or {@code null}
     */
    public LocalSigner signer() {
        return signer;
    }
    public TransportConfig getConfig() {
        return config;
    }
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
    @Override
    public void close() {
        if (listenerManager != null) listenerManager.close();
        wsTransport.close();
        rpcTransport.close();
    }
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        return mapper;
    }
    public static final class Builder {
        private String endpoint = "http://127.0.0.1:31548";
        private String wsEndpoint;
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration requestTimeout = Duration.ofSeconds(30);
        private int maxRetries = 3;
        private LocalSigner signer;
        private Builder() {}
        public Builder endpoint(String endpoint) {
            this.endpoint = Objects.requireNonNull(endpoint);
            return this;
        }
        public Builder wsEndpoint(String wsEndpoint) {
            this.wsEndpoint = wsEndpoint;
            return this;
        }
        public Builder connectTimeout(Duration timeout) {
            this.connectTimeout = timeout;
            return this;
        }
        public Builder requestTimeout(Duration timeout) {
            this.requestTimeout = timeout;
            return this;
        }
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        /**
         * Sets the local signer for offline transaction signing.
         *
         * @param signer a {@link LocalSigner} implementation (e.g., {@link io.paladin.sdk.crypto.Secp256k1Signer})
         * @return this builder
         */
        public Builder signer(LocalSigner signer) {
            this.signer = signer;
            return this;
        }
        public PaladinClient build() {
            TransportConfig config = TransportConfig.builder()
                    .httpEndpoint(endpoint)
                    .wsEndpoint(wsEndpoint)
                    .connectTimeout(connectTimeout)
                    .requestTimeout(requestTimeout)
                    .maxRetries(maxRetries)
                    .build();
            return new PaladinClient(config, signer);
        }
    }
}
