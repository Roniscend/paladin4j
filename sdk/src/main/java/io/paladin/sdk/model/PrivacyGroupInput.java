package io.paladin.sdk.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
/** Input parameters for creating a new privacy group. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PrivacyGroupInput(
        @JsonProperty("domain") String domain,
        @JsonProperty("name") String name,
        @JsonProperty("members") String[] members,
        @JsonProperty("properties") JsonNode properties
) {
    public static PrivacyGroupInput pente(String name, String... members) {
        return new PrivacyGroupInput("pente", name, members, null);
    }
}
