package io.paladin.sdk.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
/** Full transaction details including input data, receipt, and domain metadata. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionFull(
        @JsonProperty("id") String id,
        @JsonProperty("type") TransactionType type,
        @JsonProperty("domain") String domain,
        @JsonProperty("from") String from,
        @JsonProperty("to") JsonNode to,
        @JsonProperty("data") JsonNode data,
        @JsonProperty("function") String function,
        @JsonProperty("abiReference") String abiReference,
        @JsonProperty("receipt") TransactionReceipt receipt
) {}
