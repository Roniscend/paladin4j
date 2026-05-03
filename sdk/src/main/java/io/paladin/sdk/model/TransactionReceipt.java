package io.paladin.sdk.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
/** The receipt of a completed Paladin transaction, indicating success or failure. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionReceipt(
        @JsonProperty("id") String id,
        @JsonProperty("success") boolean success,
        @JsonProperty("transactionHash") String transactionHash,
        @JsonProperty("blockNumber") long blockNumber,
        @JsonProperty("domain") String domain,
        @JsonProperty("failureMessage") String failureMessage,
        @JsonProperty("contractAddress") String contractAddress,
        @JsonProperty("onChain") JsonNode onChain,
        @JsonProperty("states") JsonNode states
) {
    public boolean isFailed() {
        return !success;
    }
}
