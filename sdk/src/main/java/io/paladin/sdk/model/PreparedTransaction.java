package io.paladin.sdk.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
/** A transaction that has been prepared but not yet submitted to the ledger. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PreparedTransaction(
        @JsonProperty("id") String id,
        @JsonProperty("domain") String domain,
        @JsonProperty("to") String to,
        @JsonProperty("transaction") JsonNode transaction,
        @JsonProperty("metadata") JsonNode metadata
) {}
