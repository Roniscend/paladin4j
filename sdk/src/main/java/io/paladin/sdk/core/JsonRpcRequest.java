package io.paladin.sdk.core;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
public record JsonRpcRequest(
        @JsonProperty("jsonrpc") String jsonrpc,
        @JsonProperty("id") long id,
        @JsonProperty("method") String method,
        @JsonProperty("params") List<Object> params
) {
    private static final java.util.concurrent.atomic.AtomicLong ID_COUNTER =
            new java.util.concurrent.atomic.AtomicLong(1);
    public static JsonRpcRequest create(String method, Object... params) {
        return new JsonRpcRequest("2.0", ID_COUNTER.getAndIncrement(), method, List.of(params));
    }
    public static JsonRpcRequest create(String method, List<Object> params) {
        return new JsonRpcRequest("2.0", ID_COUNTER.getAndIncrement(), method, params);
    }
}
