package io.paladin.sdk.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
/** Represents a privacy-domain contract state (e.g., a Noto UTXO or Zeto commitment). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record State(
        @JsonProperty("id") String id,
        @JsonProperty("domainName") String domainName,
        @JsonProperty("contractAddress") String contractAddress,
        @JsonProperty("schemaId") String schemaId,
        @JsonProperty("data") JsonNode data,
        @JsonProperty("confirmed") boolean confirmed,
        @JsonProperty("spent") boolean spent
) {}
