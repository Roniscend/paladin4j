package io.paladin.sdk.core;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;/**
 * HTTP-based JSON-RPC 2.0 transport for communicating with a Paladin node.
 *
 * <p>This transport handles serialization of {@link JsonRpcRequest} objects,
 * HTTP POST delivery via the JDK 17 {@link java.net.http.HttpClient}, and
 * deserialization of {@link JsonRpcResponse} results.
 *
 * <p><strong>Retry &amp; Backoff</strong></p>
 * <p>When an HTTP 5xx server error or an {@link java.io.IOException} (connection
 * failure) is encountered, the transport will automatically retry the request up to
 * {@link TransportConfig#maxRetries()} times. The backoff delay between retries
 * increases linearly: {@code retryBackoff * (attempt + 1)}. Client errors (4xx)
 * are <b>not</b> retried.
 *
 * @see TransportConfig
 * @see JsonRpcRequest
 * @see JsonRpcResponse
 */
public final class JsonRpcTransport implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(JsonRpcTransport.class);
    private static final String CONTENT_TYPE = "application/json";
    private final HttpClient httpClient;
    private final URI endpoint;
    private final ObjectMapper objectMapper;
    private final TransportConfig config;
    public JsonRpcTransport(TransportConfig config, ObjectMapper objectMapper) {
        this.config = Objects.requireNonNull(config);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.endpoint = URI.create(config.httpEndpoint());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }
    public <T> T invoke(String method, Class<T> resultType, Object... params) {
        try {
            return invokeAsync(method, resultType, params).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof PaladinException pe) throw pe;
            throw new PaladinException("RPC call failed: " + method, e.getCause());
        }
    }
    public <T> T invoke(String method, JavaType resultType, Object... params) {
        try {
            return this.<T>invokeAsync(method, resultType, params).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof PaladinException pe) throw pe;
            throw new PaladinException("RPC call failed: " + method, e.getCause());
        }
    }
    public <T> CompletableFuture<T> invokeAsync(String method, Class<T> resultType, Object... params) {
        return executeWithRetry(method, List.of(params), 0)
                .thenApply(response -> deserializeResult(response, resultType));
    }
    public <T> CompletableFuture<T> invokeAsync(String method, JavaType resultType, Object... params) {
        return executeWithRetry(method, List.of(params), 0)
                .thenApply(response -> deserializeResult(response, resultType));
    }
    public CompletableFuture<JsonNode> invokeRaw(String method, Object... params) {
        return executeWithRetry(method, List.of(params), 0)
                .thenApply(response -> {
                    if (response.isError()) {
                        throw new PaladinException(response.error());
                    }
                    return response.result();
                });
    }
    private CompletableFuture<JsonRpcResponse> executeWithRetry(String method, List<Object> params, int attempt) {
        JsonRpcRequest rpcRequest = JsonRpcRequest.create(method, params);
        String body;
        try {
            body = objectMapper.writeValueAsString(rpcRequest);
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(
                    new PaladinException("Failed to serialize RPC request for " + method, e));
        }
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(endpoint)
                .timeout(config.requestTimeout())
                .header("Content-Type", CONTENT_TYPE)
                .header("Accept", CONTENT_TYPE)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        log.debug("RPC request [{}] id={}: {}", method, rpcRequest.id(), body);
        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenCompose(httpResponse -> {
                    log.debug("RPC response [{}] id={}: status={}", method, rpcRequest.id(),
                            httpResponse.statusCode());
                    if (httpResponse.statusCode() >= 500 && attempt < config.maxRetries()) {
                        log.warn("Retrying RPC call {} (attempt {}/{})", method, attempt + 1,
                                config.maxRetries());
                        return delay(config.retryBackoff().multipliedBy(attempt + 1))
                                .thenCompose(v -> executeWithRetry(method, params, attempt + 1));
                    }
                    if (httpResponse.statusCode() != 200) {
                        return CompletableFuture.failedFuture(
                                new PaladinException("HTTP %d from %s: %s".formatted(
                                        httpResponse.statusCode(), method, httpResponse.body())));
                    }
                    try {
                        JsonRpcResponse rpcResponse = objectMapper.readValue(
                                httpResponse.body(), JsonRpcResponse.class);
                        return CompletableFuture.completedFuture(rpcResponse);
                    } catch (JsonProcessingException e) {
                        return CompletableFuture.failedFuture(
                                new PaladinException("Failed to parse RPC response for " + method, e));
                    }
                })
                .exceptionallyCompose(throwable -> {
                    if (throwable.getCause() instanceof IOException && attempt < config.maxRetries()) {
                        log.warn("Connection error for {}, retrying ({}/{})", method, attempt + 1,
                                config.maxRetries());
                        return delay(config.retryBackoff().multipliedBy(attempt + 1))
                                .thenCompose(v -> executeWithRetry(method, params, attempt + 1));
                    }
                    return CompletableFuture.failedFuture(throwable);
                });
    }
    private <T> T deserializeResult(JsonRpcResponse response, Class<T> type) {
        if (response.isError()) {
            throw new PaladinException(response.error());
        }
        try {
            if (type == Void.class || type == void.class) {
                return null;
            }
            return objectMapper.treeToValue(response.result(), type);
        } catch (JsonProcessingException e) {
            throw new PaladinException("Failed to deserialize result as " + type.getSimpleName(), e);
        }
    }
    private <T> T deserializeResult(JsonRpcResponse response, JavaType type) {
        if (response.isError()) {
            throw new PaladinException(response.error());
        }
        try {
            return objectMapper.readValue(
                    objectMapper.treeAsTokens(response.result()),
                    type
            );
        } catch (IOException e) {
            throw new PaladinException("Failed to deserialize result as " + type, e);
        }
    }
    private static CompletableFuture<Void> delay(Duration duration) {
        return CompletableFuture.runAsync(() -> {},
                CompletableFuture.delayedExecutor(duration.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS));
    }
    @Override
    public void close() {
        // HttpClient does not implement AutoCloseable until JDK 21.
        // On JDK 17, there is no public API to shut down its internal executor.
        // When the SDK baseline moves to JDK 21+, call httpClient.close() here.
    }
}
