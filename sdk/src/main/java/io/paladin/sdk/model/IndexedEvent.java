package io.paladin.sdk.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
/** A decoded event log entry from the Paladin block indexer. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IndexedEvent(
        @JsonProperty("blockNumber") long blockNumber,
        @JsonProperty("transactionIndex") int transactionIndex,
        @JsonProperty("logIndex") int logIndex,
        @JsonProperty("transactionHash") String transactionHash,
        @JsonProperty("signature") String signature,
        @JsonProperty("address") String address,
        @JsonProperty("data") JsonNode data
) {}
