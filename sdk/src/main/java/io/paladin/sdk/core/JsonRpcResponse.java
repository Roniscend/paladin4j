package io.paladin.sdk.core;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
@JsonIgnoreProperties(ignoreUnknown = true)
public record JsonRpcResponse(
        @JsonProperty("jsonrpc") String jsonrpc,
        @JsonProperty("id") long id,
        @JsonProperty("result") JsonNode result,
        @JsonProperty("error") JsonRpcError error
) {
    public boolean isError() {
        return error != null;
    }
    public boolean isSuccess() {
        return error == null && result != null;
    }
}
