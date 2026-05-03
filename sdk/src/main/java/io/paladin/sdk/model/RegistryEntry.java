package io.paladin.sdk.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
/** Represents an entry in the Paladin on-chain registry. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RegistryEntry(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("registry") String registry,
        @JsonProperty("parentId") String parentId,
        @JsonProperty("active") boolean active,
        @JsonProperty("properties") JsonNode properties
) {}
