package io.paladin.sdk.core;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
public final class WebSocketTransport implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(WebSocketTransport.class);
    private static final Duration RECONNECT_DELAY = Duration.ofSeconds(3);
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private final URI wsEndpoint;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final TransportConfig config;
    private final AtomicReference<WebSocket> activeSocket = new AtomicReference<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Map<String, Consumer<JsonNode>> subscriptions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService reconnectExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "paladin-ws-reconnect");
                t.setDaemon(true);
                return t;
            });
    public WebSocketTransport(TransportConfig config, ObjectMapper objectMapper) {
        this.config = Objects.requireNonNull(config);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.wsEndpoint = URI.create(config.wsEndpoint());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .build();
    }
    public CompletableFuture<Void> connect() {
        if (closed.get()) {
            return CompletableFuture.failedFuture(
                    new PaladinException("WebSocket transport is closed"));
        }
        return doConnect(0);
    }
    private CompletableFuture<Void> doConnect(int attempt) {
        log.info("Connecting WebSocket to {} (attempt {})", wsEndpoint, attempt + 1);
        return httpClient.newWebSocketBuilder()
                .connectTimeout(config.connectTimeout())
                .buildAsync(wsEndpoint, new WebSocket.Listener() {
                    private final StringBuilder messageBuffer = new StringBuilder();
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        log.info("WebSocket connected to {}", wsEndpoint);
                        activeSocket.set(webSocket);
                        webSocket.request(1);
                    }
                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        messageBuffer.append(data);
                        if (last) {
                            String fullMessage = messageBuffer.toString();
                            messageBuffer.setLength(0);
                            handleMessage(fullMessage);
                        }
                        webSocket.request(1);
                        return CompletableFuture.completedFuture(null);
                    }
                    @Override
                    public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
                        webSocket.sendPong(message);
                        webSocket.request(1);
                        return CompletableFuture.completedFuture(null);
                    }
                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        log.warn("WebSocket closed: {} {}", statusCode, reason);
                        activeSocket.set(null);
                        if (!closed.get()) {
                            scheduleReconnect();
                        }
                        return CompletableFuture.completedFuture(null);
                    }
                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        log.error("WebSocket error", error);
                        activeSocket.set(null);
                        if (!closed.get()) {
                            scheduleReconnect();
                        }
                    }
                })
                .thenAccept(ws -> {
                })
                .exceptionallyCompose(ex -> {
                    if (attempt < MAX_RECONNECT_ATTEMPTS && !closed.get()) {
                        log.warn("WebSocket connect failed (attempt {}/{}), retrying...",
                                attempt + 1, MAX_RECONNECT_ATTEMPTS, ex);
                        return delay(RECONNECT_DELAY.multipliedBy(attempt + 1))
                                .thenCompose(v -> doConnect(attempt + 1));
                    }
                    return CompletableFuture.failedFuture(
                            new PaladinException("WebSocket connection failed after " +
                                    (attempt + 1) + " attempts", ex));
                });
    }
    public void subscribe(String subscriptionId, Consumer<JsonNode> handler) {
        subscriptions.put(subscriptionId, handler);
        log.debug("Registered subscription: {}", subscriptionId);
    }
    public void unsubscribe(String subscriptionId) {
        subscriptions.remove(subscriptionId);
        log.debug("Removed subscription: {}", subscriptionId);
    }
    public CompletableFuture<Void> send(JsonRpcRequest request) {
        WebSocket ws = activeSocket.get();
        if (ws == null) {
            return CompletableFuture.failedFuture(
                    new PaladinException("WebSocket not connected"));
        }
        try {
            String payload = objectMapper.writeValueAsString(request);
            log.debug("WS send: {}", payload);
            return ws.sendText(payload, true).thenAccept(w -> {});
        } catch (Exception e) {
            return CompletableFuture.failedFuture(
                    new PaladinException("Failed to serialize WS request", e));
        }
    }
    public boolean isConnected() {
        WebSocket ws = activeSocket.get();
        return ws != null && !ws.isInputClosed() && !ws.isOutputClosed();
    }
    public int getSubscriptionCount() {
        return subscriptions.size();
    }
    private void handleMessage(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            log.debug("WS received: {}", message);
            if (node.has("method") && node.has("params")) {
                String method = node.get("method").asText();
                JsonNode params = node.get("params");
                String subscriptionId = params.has("subscription")
                        ? params.get("subscription").asText()
                        : method;
                Consumer<JsonNode> handler = subscriptions.get(subscriptionId);
                if (handler != null) {
                    handler.accept(params.has("result") ? params.get("result") : params);
                } else {
                    log.debug("No handler for subscription: {}", subscriptionId);
                }
            }
            if (node.has("id") && (node.has("result") || node.has("error"))) {
                log.debug("WS response id={}", node.get("id"));
            }
        } catch (Exception e) {
            log.error("Failed to handle WebSocket message: {}", message, e);
        }
    }
    private void scheduleReconnect() {
        if (closed.get()) return;
        log.info("Scheduling WebSocket reconnection in {}s", RECONNECT_DELAY.toSeconds());
        reconnectExecutor.schedule(() -> {
            if (!closed.get()) {
                connect().exceptionally(ex -> {
                    log.error("Reconnection failed", ex);
                    return null;
                });
            }
        }, RECONNECT_DELAY.toSeconds(), TimeUnit.SECONDS);
    }
    private static CompletableFuture<Void> delay(Duration duration) {
        return CompletableFuture.runAsync(() -> {},
                CompletableFuture.delayedExecutor(duration.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS));
    }
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            log.info("Closing WebSocket transport");
            reconnectExecutor.shutdownNow();
            WebSocket ws = activeSocket.getAndSet(null);
            if (ws != null) {
                try {
                    ws.sendClose(WebSocket.NORMAL_CLOSURE, "Client closing")
                            .orTimeout(5, TimeUnit.SECONDS)
                            .exceptionally(ex -> null)
                            .join();
                } catch (Exception ignored) {
                    // Best-effort close — don't propagate errors during shutdown
                }
            }
            subscriptions.clear();
        }
    }
}
