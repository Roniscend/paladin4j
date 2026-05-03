package io.paladin.sdk.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
/** Represents a schema definition registered within a privacy domain. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Schema(
        @JsonProperty("id") String id,
        @JsonProperty("domainName") String domainName,
        @JsonProperty("type") String type,
        @JsonProperty("signature") String signature,
        @JsonProperty("definition") JsonNode definition,
        @JsonProperty("labels") String[] labels
) {}
