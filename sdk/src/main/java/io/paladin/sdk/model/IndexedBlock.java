package io.paladin.sdk.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
/** An indexed block header from the Paladin block indexer. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IndexedBlock(
        @JsonProperty("number") long number,
        @JsonProperty("hash") String hash,
        @JsonProperty("timestamp") long timestamp,
        @JsonProperty("transactionCount") int transactionCount
) {}
