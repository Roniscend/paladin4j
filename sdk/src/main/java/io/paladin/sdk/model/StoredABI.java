package io.paladin.sdk.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
/** Represents a stored ABI (Application Binary Interface) identified by its hash. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StoredABI(
        @JsonProperty("hash") String hash,
        @JsonProperty("abi") JsonNode abi
) {}
