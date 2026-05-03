package io.paladin.sdk.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
/** A privacy group comprising a set of members sharing encrypted state. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PrivacyGroup(
        @JsonProperty("id") String id,
        @JsonProperty("domain") String domain,
        @JsonProperty("name") String name,
        @JsonProperty("members") String[] members,
        @JsonProperty("salt") String salt,
        @JsonProperty("contractAddress") String contractAddress,
        @JsonProperty("properties") JsonNode properties
) {}
